package com.example.dualscreen.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.dualscreen.data.CalibrationProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections

enum class DeviceRole {
    NONE,
    HOST,
    CLIENT
}

enum class ConnectionState {
    DISCONNECTED,
    DISCOVERING,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

class NetworkManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _role = MutableStateFlow(DeviceRole.NONE)
    val role: StateFlow<DeviceRole> = _role.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _localIp = MutableStateFlow(getLocalIpAddress())
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    private val _connectedPeerIp = MutableStateFlow<String?>(null)
    val connectedPeerIp: StateFlow<String?> = _connectedPeerIp.asStateFlow()

    private val _latencyMs = MutableStateFlow(0L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private val _fpsCount = MutableStateFlow(30)
    val fpsCount: StateFlow<Int> = _fpsCount.asStateFlow()

    private val _bandwidthKbps = MutableStateFlow(0.0f)
    val bandwidthKbps: StateFlow<Float> = _bandwidthKbps.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready to connect")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Live synced states
    private val _config = MutableStateFlow(ConfigPayload())
    val config: StateFlow<ConfigPayload> = _config.asStateFlow()

    private val _calibration = MutableStateFlow(CalibrationPayload())
    val calibration: StateFlow<CalibrationPayload> = _calibration.asStateFlow()

    private val _videoSync = MutableStateFlow(VideoSyncPayload(isPlaying = false, positionMs = 0L))
    val videoSync: StateFlow<VideoSyncPayload> = _videoSync.asStateFlow()

    private val _latestTouch = MutableStateFlow<TouchPayload?>(null)
    val latestTouch: StateFlow<TouchPayload?> = _latestTouch.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var activeSocket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    private var pingJob: Job? = null
    private val PORT = 8888

    init {
        refreshLocalIp()
    }

    fun refreshLocalIp() {
        _localIp.value = getLocalIpAddress()
    }

    fun startHost() {
        scope.launch {
            try {
                stopConnections()
                _role.value = DeviceRole.HOST
                _connectionState.value = ConnectionState.CONNECTING
                _statusMessage.value = "Starting Host server on port $PORT..."

                serverSocket = ServerSocket(PORT)
                _statusMessage.value = "Host running at ${_localIp.value}:$PORT. Waiting for Client..."

                val client = serverSocket?.accept() ?: return@launch
                setupSocketSession(client, DeviceRole.HOST)
            } catch (e: Exception) {
                Log.e("NetworkManager", "Error starting host", e)
                _connectionState.value = ConnectionState.ERROR
                _statusMessage.value = "Host error: ${e.message}"
            }
        }
    }

    fun connectToHost(hostIp: String) {
        scope.launch {
            try {
                stopConnections()
                _role.value = DeviceRole.CLIENT
                _connectionState.value = ConnectionState.CONNECTING
                _statusMessage.value = "Connecting to Host at $hostIp:$PORT..."

                val socket = Socket(hostIp, PORT)
                setupSocketSession(socket, DeviceRole.CLIENT)
            } catch (e: Exception) {
                Log.e("NetworkManager", "Error connecting to host", e)
                _connectionState.value = ConnectionState.ERROR
                _statusMessage.value = "Failed to connect to $hostIp: ${e.message}"
            }
        }
    }

    private suspend fun setupSocketSession(socket: Socket, currentRole: DeviceRole) {
        activeSocket = socket
        writer = PrintWriter(socket.getOutputStream(), true)
        reader = BufferedReader(InputStreamReader(socket.getInputStream()))

        _connectedPeerIp.value = socket.inetAddress.hostAddress
        _connectionState.value = ConnectionState.CONNECTED
        _statusMessage.value = "Connected to ${_connectedPeerIp.value}"

        // Send initial Handshake
        sendPacket(
            DualScreenPacket(
                type = PacketType.HANDSHAKE,
                senderRole = currentRole.name,
                payloadJson = "{\"ip\":\"${_localIp.value}\"}"
            )
        )

        // Start ping loop for RTT measurement
        startPingLoop()

        // Read loop
        withContext(Dispatchers.IO) {
            try {
                var line: String?
                while (reader?.readLine().also { line = it } != null) {
                    val rawLine = line ?: break
                    val packet = DualScreenPacket.fromJsonString(rawLine) ?: continue
                    handleIncomingPacket(packet)
                }
            } catch (e: Exception) {
                Log.e("NetworkManager", "Socket read loop exception", e)
            } finally {
                onDisconnected("Connection closed by peer")
            }
        }
    }

    private fun handleIncomingPacket(packet: DualScreenPacket) {
        when (packet.type) {
            PacketType.HANDSHAKE -> {
                Log.d("NetworkManager", "Received handshake from ${packet.senderRole}")
            }
            PacketType.PING -> {
                // Respond immediately with PONG
                sendPacket(
                    DualScreenPacket(
                        type = PacketType.PONG,
                        senderRole = _role.value.name,
                        timestampMs = packet.timestampMs
                    )
                )
            }
            PacketType.PONG -> {
                val roundTripTime = System.currentTimeMillis() - packet.timestampMs
                _latencyMs.value = roundTripTime / 2
            }
            PacketType.CONFIG_SYNC -> {
                val newConfig = ConfigPayload.fromJson(packet.payloadJson)
                _config.value = newConfig
            }
            PacketType.CALIBRATION_SYNC -> {
                val newCalib = CalibrationPayload.fromJson(packet.payloadJson)
                _calibration.value = newCalib
            }
            PacketType.VIDEO_SYNC -> {
                val newVideoSync = VideoSyncPayload.fromJson(packet.payloadJson)
                _videoSync.value = newVideoSync
            }
            PacketType.TOUCH_EVENT -> {
                val touch = TouchPayload.fromJson(packet.payloadJson)
                _latestTouch.value = touch
            }
            PacketType.DISCONNECT -> {
                onDisconnected("Peer requested disconnect")
            }
            else -> {}
        }
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                sendPacket(
                    DualScreenPacket(
                        type = PacketType.PING,
                        senderRole = _role.value.name
                    )
                )
                delay(1000)
            }
        }
    }

    fun sendConfig(newConfig: ConfigPayload) {
        _config.value = newConfig
        sendPacket(
            DualScreenPacket(
                type = PacketType.CONFIG_SYNC,
                senderRole = _role.value.name,
                payloadJson = newConfig.toJson()
            )
        )
    }

    fun sendCalibration(newCalib: CalibrationPayload) {
        _calibration.value = newCalib
        sendPacket(
            DualScreenPacket(
                type = PacketType.CALIBRATION_SYNC,
                senderRole = _role.value.name,
                payloadJson = newCalib.toJson()
            )
        )
    }

    fun sendVideoSync(isPlaying: Boolean, positionMs: Long) {
        val vs = VideoSyncPayload(isPlaying, positionMs)
        _videoSync.value = vs
        sendPacket(
            DualScreenPacket(
                type = PacketType.VIDEO_SYNC,
                senderRole = _role.value.name,
                payloadJson = vs.toJson()
            )
        )
    }

    fun sendTouchEvent(action: String, xRatio: Float, yRatio: Float) {
        val touch = TouchPayload(action, xRatio, yRatio)
        _latestTouch.value = touch
        sendPacket(
            DualScreenPacket(
                type = PacketType.TOUCH_EVENT,
                senderRole = _role.value.name,
                payloadJson = touch.toJson()
            )
        )
    }

    private fun sendPacket(packet: DualScreenPacket) {
        scope.launch(Dispatchers.IO) {
            try {
                writer?.println(packet.toJsonString())
            } catch (e: Exception) {
                Log.e("NetworkManager", "Failed to send packet", e)
            }
        }
    }

    fun stopConnections() {
        pingJob?.cancel()
        try {
            writer?.close()
            reader?.close()
            activeSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e("NetworkManager", "Error closing sockets", e)
        }
        activeSocket = null
        serverSocket = null
        writer = null
        reader = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectedPeerIp.value = null
        _statusMessage.value = "Disconnected"
    }

    private fun onDisconnected(reason: String) {
        stopConnections()
        _statusMessage.value = "Disconnected: $reason"
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: ""
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkManager", "Error getting IP", e)
        }
        return "127.0.0.1"
    }
}
