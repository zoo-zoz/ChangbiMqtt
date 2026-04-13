package uni.changbi.mqtt

import android.content.Context
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.util.concurrent.Executors

/**
 * MQTT 辅助类 - 使用 Eclipse Paho Android Service
 * 支持 TCP (mqtt://, mqtts://) 和 WebSocket (ws://, wss://) 连接
 */
class MqttHelper(private val context: Context) {
    
    private var mqttClient: MqttAndroidClient? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var connectCallback: ((Boolean, String?) -> Unit)? = null
    private var messageCallback: ((String, String, Int, Boolean) -> Unit)? = null
    private var closeCallback: (() -> Unit)? = null
    private var connectionLostCallback: (() -> Unit)? = null
    
    /**
     * 设置连接回调
     */
    fun setConnectCallback(callback: (Boolean, String?) -> Unit) {
        this.connectCallback = callback
    }
    
    /**
     * 设置消息回调
     */
    fun setMessageCallback(callback: (String, String, Int, Boolean) -> Unit) {
        this.messageCallback = callback
    }
    
    /**
     * 设置关闭回调
     */
    fun setCloseCallback(callback: () -> Unit) {
        this.closeCallback = callback
    }
    
    /**
     * 设置连接丢失回调
     */
    fun setConnectionLostCallback(callback: () -> Unit) {
        this.connectionLostCallback = callback
    }
    
    /**
     * 连接到 MQTT Broker
     * 支持协议：mqtt://, mqtts://, ws://, wss://
     * 
     * Paho Android Service 原生支持 WebSocket！
     * 只需要将 URL 改为 ws:// 或 wss:// 即可
     */
    fun connect(
        url: String,
        clientId: String,
        username: String?,
        password: String?,
        clean: Boolean,
        timeout: Int,
        keepalive: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        executor.execute {
            try {
                // 构建服务器 URI
                // Paho Android Service 支持：
                // - tcp://host:port (MQTT over TCP)
                // - ssl://host:port (MQTT over TLS)
                // - ws://host:port/path (MQTT over WebSocket)
                // - wss://host:port/path (MQTT over WebSocket Secure)
                val serverUri = normalizeServerUri(url)
                
                // 创建 MqttAndroidClient
                mqttClient = MqttAndroidClient(context, serverUri, clientId)
                
                // 设置回调
                mqttClient?.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        closeCallback?.invoke()
                        // 触发连接丢失回调（用于自动重连）
                        connectionLostCallback?.invoke()
                    }
                    
                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        if (topic != null && message != null) {
                            val payload = String(message.payload)
                            val qos = message.qos
                            val retain = message.isRetained
                            messageCallback?.invoke(topic, payload, qos, retain)
                        }
                    }
                    
                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        // 消息发送完成
                    }
                })
                
                // 配置连接选项
                val options = MqttConnectOptions().apply {
                    isCleanSession = clean
                    connectionTimeout = timeout / 1000
                    keepAliveInterval = keepalive
                    
                    if (username != null) {
                        userName = username
                        if (password != null) {
                            setPassword(password.toCharArray())
                        }
                    }
                }
                
                // 连接
                mqttClient?.connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        // asyncActionToken 可能为 null，需要安全处理
                        val sessionPresent = try {
                            asyncActionToken?.sessionPresent ?: false
                        } catch (e: Exception) {
                            false
                        }
                        connectCallback?.invoke(sessionPresent, null)
                        onSuccess()
                    }
                    
                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        // 获取详细的错误信息
                        val errorMsg = when {
                            exception == null -> "Connection failed (unknown error)"
                            exception is MqttException -> {
                                val reasonCode = exception.reasonCode
                                val message = exception.message ?: "Unknown MQTT error"
                                val cause = exception.cause
                                
                                val details = StringBuilder()
                                details.append("MQTT Error (code: $reasonCode)")
                                
                                // 添加错误码说明
                                when (reasonCode) {
                                    0 -> {
                                        details.append(" - Connection refused or network error")
                                        // 对于 WSS 连接，提供更详细的诊断信息
                                        if (url.startsWith("wss://")) {
                                            details.append("\n💡 WSS 连接失败可能原因：")
                                            details.append("\n  1. SSL 证书问题（自签名证书、证书过期、域名不匹配）")
                                            details.append("\n  2. 服务器未正确配置 SSL/TLS")
                                            details.append("\n  3. 网络防火墙阻止了 8084 端口")
                                            details.append("\n  4. 服务器地址或端口错误")
                                            details.append("\n\n🔧 建议尝试：")
                                            details.append("\n  • 使用 WS (非加密): ws://192.168.5.238:8083/mqtt")
                                            details.append("\n  • 检查服务器 SSL 证书配置")
                                            details.append("\n  • 确认服务器 8084 端口已开放")
                                        }
                                    }
                                    32100 -> details.append(" - Client is not connected")
                                    32101 -> details.append(" - Client is already connected")
                                    32102 -> details.append(" - Client is disconnecting")
                                    32103 -> details.append(" - Server unavailable")
                                    32104 -> details.append(" - Bad username or password")
                                    32105 -> details.append(" - Not authorized")
                                    32109 -> details.append(" - Connection lost")
                                }
                                
                                details.append("\n\n📋 错误详情: $message")
                                
                                // 添加原因链 - 深入挖掘所有层级的异常
                                var currentCause = cause
                                var level = 1
                                while (currentCause != null && level <= 3) {
                                    val causeMsg = currentCause.message ?: currentCause.javaClass.simpleName
                                    details.append("\n  └─ Cause $level: $causeMsg")
                                    currentCause = currentCause.cause
                                    level++
                                }
                                
                                details.toString()
                            }
                            else -> {
                                val message = exception.message ?: "Connection failed"
                                val cause = exception.cause
                                if (cause != null) {
                                    "$message | Cause: ${cause.message ?: cause.toString()}"
                                } else {
                                    message
                                }
                            }
                        }
                        onError(errorMsg)
                    }
                })
                
            } catch (e: Exception) {
                // 获取详细的异常信息
                val errorMsg = when (e) {
                    is MqttException -> {
                        val reasonCode = e.reasonCode
                        val message = e.message ?: "Unknown MQTT error"
                        val cause = e.cause
                        
                        val details = StringBuilder()
                        details.append("MQTT Error (code: $reasonCode)")
                        
                        when (reasonCode) {
                            0 -> {
                                details.append(" - Connection refused or network error")
                                if (url.startsWith("wss://")) {
                                    details.append("\n💡 WSS 连接失败可能原因：")
                                    details.append("\n  1. SSL 证书问题（自签名证书、证书过期、域名不匹配）")
                                    details.append("\n  2. 服务器未正确配置 SSL/TLS")
                                    details.append("\n  3. 网络防火墙阻止了端口")
                                    details.append("\n\n🔧 建议尝试 WS (非加密) 连接")
                                }
                            }
                            32100 -> details.append(" - Client is not connected")
                            32103 -> details.append(" - Server unavailable")
                        }
                        
                        details.append("\n\n📋 错误详情: $message")
                        
                        // 添加完整的异常链
                        var currentCause = cause
                        var level = 1
                        while (currentCause != null && level <= 3) {
                            val causeMsg = currentCause.message ?: currentCause.javaClass.simpleName
                            details.append("\n  └─ Cause $level: $causeMsg")
                            currentCause = currentCause.cause
                            level++
                        }
                        
                        details.toString()
                    }
                    else -> {
                        val message = e.message ?: "Connection failed"
                        val details = StringBuilder(message)
                        
                        // 添加完整的异常链
                        var currentCause = e.cause
                        var level = 1
                        while (currentCause != null && level <= 3) {
                            val causeMsg = currentCause.message ?: currentCause.javaClass.simpleName
                            details.append("\n  └─ Cause $level: $causeMsg")
                            currentCause = currentCause.cause
                            level++
                        }
                        
                        details.toString()
                    }
                }
                onError(errorMsg)
            }
        }
    }
    
    /**
     * 断开连接
     */
    fun disconnect(onSuccess: () -> Unit, onError: (String) -> Unit) {
        executor.execute {
            try {
                mqttClient?.disconnect(null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        closeCallback?.invoke()
                        onSuccess()
                    }
                    
                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val errorMsg = getErrorMessage(exception, "Disconnect failed")
                        onError(errorMsg)
                    }
                })
            } catch (e: Exception) {
                val errorMsg = getErrorMessage(e, "Disconnect failed")
                onError(errorMsg)
            }
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        try {
            mqttClient?.disconnect()
            mqttClient?.close()
            mqttClient = null
        } catch (e: Exception) {
            // 忽略清理错误
        }
    }
    
    /**
     * 检查是否已连接
     */
    fun isConnected(): Boolean {
        return mqttClient?.isConnected ?: false
    }
    
    /**
     * 订阅主题
     */
    fun subscribe(topic: String, qos: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        executor.execute {
            try {
                if (mqttClient?.isConnected != true) {
                    onError("Not connected")
                    return@execute
                }
                
                mqttClient?.subscribe(topic, qos, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        onSuccess()
                    }
                    
                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val errorMsg = getErrorMessage(exception, "Subscribe failed")
                        onError(errorMsg)
                    }
                })
                
            } catch (e: Exception) {
                val errorMsg = getErrorMessage(e, "Subscribe failed")
                onError(errorMsg)
            }
        }
    }
    
    /**
     * 取消订阅
     */
    fun unsubscribe(topic: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        executor.execute {
            try {
                if (mqttClient?.isConnected != true) {
                    onError("Not connected")
                    return@execute
                }
                
                mqttClient?.unsubscribe(topic, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        onSuccess()
                    }
                    
                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val errorMsg = getErrorMessage(exception, "Unsubscribe failed")
                        onError(errorMsg)
                    }
                })
                
            } catch (e: Exception) {
                val errorMsg = getErrorMessage(e, "Unsubscribe failed")
                onError(errorMsg)
            }
        }
    }
    
    /**
     * 发布消息
     */
    fun publish(
        topic: String,
        payload: String,
        qos: Int,
        retain: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        executor.execute {
            try {
                if (mqttClient?.isConnected != true) {
                    onError("Not connected")
                    return@execute
                }
                
                val message = MqttMessage().apply {
                    this.payload = payload.toByteArray()
                    this.qos = qos
                    this.isRetained = retain
                }
                
                mqttClient?.publish(topic, message, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        onSuccess()
                    }
                    
                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val errorMsg = getErrorMessage(exception, "Publish failed")
                        onError(errorMsg)
                    }
                })
                
            } catch (e: Exception) {
                val errorMsg = getErrorMessage(e, "Publish failed")
                onError(errorMsg)
            }
        }
    }
    
    /**
     * 获取详细的错误信息
     */
    private fun getErrorMessage(exception: Throwable?, defaultMessage: String): String {
        return when {
            exception == null -> defaultMessage
            exception is MqttException -> {
                val reasonCode = exception.reasonCode
                val message = exception.message ?: "Unknown error"
                val cause = exception.cause?.message
                
                val errorDetails = StringBuilder()
                errorDetails.append("MQTT Error (code: $reasonCode)")
                
                // 添加常见错误码的说明
                val codeDescription = when (reasonCode) {
                    32100 -> "Client is not connected"
                    32101 -> "Client is already connected"
                    32102 -> "Client is disconnecting"
                    32103 -> "Server unavailable"
                    32104 -> "Bad username or password"
                    32105 -> "Not authorized"
                    32109 -> "Connection lost"
                    32110 -> "Unexpected error"
                    32111 -> "Client exception"
                    else -> null
                }
                
                if (codeDescription != null) {
                    errorDetails.append(" - $codeDescription")
                }
                
                errorDetails.append(": $message")
                
                if (cause != null && cause != message) {
                    errorDetails.append(" (Cause: $cause)")
                }
                
                errorDetails.toString()
            }
            else -> {
                val message = exception.message ?: defaultMessage
                val cause = exception.cause?.message
                if (cause != null && cause != message) {
                    "$message (Cause: $cause)"
                } else {
                    message
                }
            }
        }
    }
    
    /**
     * 规范化服务器 URI
     * 
     * Paho Android Service 支持的 URI 格式：
     * - tcp://host:port
     * - ssl://host:port
     * - ws://host:port/path
     * - wss://host:port/path
     * 
     * 将 mqtt:// 转换为 tcp://，mqtts:// 转换为 ssl://
     * ws:// 和 wss:// 保持不变（原生支持）
     */
    private fun normalizeServerUri(url: String): String {
        return when {
            url.startsWith("mqtt://") -> url.replace("mqtt://", "tcp://")
            url.startsWith("mqtts://") -> url.replace("mqtts://", "ssl://")
            url.startsWith("ws://") -> url  // WebSocket - 原生支持
            url.startsWith("wss://") -> url // WebSocket Secure - 原生支持
            else -> url
        }
    }
}
