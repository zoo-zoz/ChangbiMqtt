package uni.changbi.mqtt

import android.content.Context
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.concurrent.Executors

/**
 * MQTT 辅助类 - 使用纯 Java MQTT 客户端（兼容 Android 12+）
 */
class MqttHelper(private val context: Context) {
    
    private var mqttClient: MqttClient? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var connectCallback: ((Boolean, String?) -> Unit)? = null
    private var messageCallback: ((String, String, Int, Boolean) -> Unit)? = null
    private var closeCallback: (() -> Unit)? = null
    
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
     * 连接到 MQTT Broker
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
                // 转换 URL 协议：mqtt:// -> tcp://, mqtts:// -> ssl://
                val serverUri = when {
                    url.startsWith("mqtt://") -> url.replace("mqtt://", "tcp://")
                    url.startsWith("mqtts://") -> url.replace("mqtts://", "ssl://")
                    else -> url
                }
                
                // 使用内存持久化
                val persistence = MemoryPersistence()
                mqttClient = MqttClient(serverUri, clientId, persistence)
                
                // 设置回调
                mqttClient?.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        closeCallback?.invoke()
                    }
                    
                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        if (topic != null && message != null) {
                            val payload = String(message.payload)
                            messageCallback?.invoke(topic, payload, message.qos, message.isRetained)
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
                    isAutomaticReconnect = false
                    
                    if (username != null) {
                        userName = username
                    }
                    if (password != null) {
                        setPassword(password.toCharArray())
                    }
                }
                
                // 连接
                mqttClient?.connect(options)
                
                // 连接成功
                val sessionPresent = !clean
                connectCallback?.invoke(sessionPresent, null)
                onSuccess()
                
            } catch (e: Exception) {
                onError(e.message ?: "Connection failed")
            }
        }
    }
    
    /**
     * 断开连接
     */
    fun disconnect(onSuccess: () -> Unit, onError: (String) -> Unit) {
        executor.execute {
            try {
                if (mqttClient?.isConnected == true) {
                    mqttClient?.disconnect()
                }
                // 主动触发关闭回调
                closeCallback?.invoke()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Disconnect failed")
            }
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        try {
            if (mqttClient?.isConnected == true) {
                mqttClient?.disconnect()
            }
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
                
                mqttClient?.subscribe(topic, qos)
                onSuccess()
                
            } catch (e: Exception) {
                onError(e.message ?: "Subscribe failed")
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
                
                mqttClient?.unsubscribe(topic)
                onSuccess()
                
            } catch (e: Exception) {
                onError(e.message ?: "Unsubscribe failed")
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
                
                val message = MqttMessage(payload.toByteArray()).apply {
                    this.qos = qos
                    isRetained = retain
                }
                
                mqttClient?.publish(topic, message)
                onSuccess()
                
            } catch (e: Exception) {
                onError(e.message ?: "Publish failed")
            }
        }
    }
}
