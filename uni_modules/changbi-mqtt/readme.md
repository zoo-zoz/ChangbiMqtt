# Changbi MQTT 插件

## 简介

Changbi MQTT 是一个用于 UniApp 的 MQTT 客户端 UTS 插件，提供稳定、高性能的 MQTT 连接功能，支持 Android 平台。

## 特性

- ✅ 支持 MQTT 3.1.1 协议
- ✅ 支持 QoS 0、1、2 三个服务质量等级
- ✅ 支持自动重连机制
- ✅ 支持连接保活（Keepalive）
- ✅ 支持 SSL/TLS 加密连接
- ✅ 支持 WebSocket 和 WebSocket Secure
- ✅ 支持会话持久化
- ✅ 支持 Will 消息
- ✅ 支持主题通配符（+ 和 #）
- ✅ 原生代码实现，性能优异

## 平台支持

- Android ✅
- iOS ❌（暂不支持）

## 安装

将插件导入到您的 UniApp 项目的 `uni_modules` 目录下。

## 使用示例

```javascript
import mqtt from '@/uni_modules/changbi-mqtt'

// 连接配置
const options = {
  url: 'mqtt://broker.example.com:1883',
  clientId: 'client_' + Date.now(),
  username: 'your_username',
  password: 'your_password',
  clean: true,
  keepalive: 60,
  reconnect: true,
  reconnectPeriod: 1000
}

// 监听连接事件
mqtt.on('connect', (data) => {
  console.log('连接成功', data)
  
  // 订阅主题
  mqtt.subscribe('test/topic', 0)
})

// 监听消息事件
mqtt.on('message', (data) => {
  console.log('收到消息:', data.topic, data.payload)
})

// 监听错误事件
mqtt.on('error', (data) => {
  console.error('连接错误:', data.error)
})

// 连接到 Broker
mqtt.connect(options)

// 发布消息
mqtt.publish('test/topic', 'Hello MQTT', { qos: 0, retain: false })

// 断开连接
mqtt.disconnect()
```

## API 文档

### connect(options)

连接到 MQTT Broker。

**参数:**
- `options` (ConnectionOptions): 连接配置选项

**返回:** Promise<void>

### disconnect()

断开 MQTT 连接。

**返回:** Promise<void>

### subscribe(topic, qos)

订阅主题。

**参数:**
- `topic` (string | string[]): 主题名称或主题数组
- `qos` (0 | 1 | 2): 服务质量等级（可选，默认 0）

**返回:** Promise<void>

### unsubscribe(topic)

取消订阅主题。

**参数:**
- `topic` (string | string[]): 主题名称或主题数组

**返回:** Promise<void>

### publish(topic, message, options)

发布消息。

**参数:**
- `topic` (string): 主题名称
- `message` (string | ArrayBuffer): 消息内容
- `options` (PublishOptions): 发布选项（可选）

**返回:** Promise<void>

### on(event, callback)

注册事件监听器。

**参数:**
- `event` (MQTTEvent): 事件类型
- `callback` (EventCallback): 回调函数

### off(event, callback)

移除事件监听器。

**参数:**
- `event` (MQTTEvent): 事件类型
- `callback` (EventCallback): 回调函数（可选，不传则移除所有监听器）

### isConnected()

检查是否已连接。

**返回:** boolean

### getConnectionState()

获取连接状态。

**返回:** ConnectionState

### setLogLevel(level)

设置日志级别。

**参数:**
- `level` (LogLevel): 日志级别

## 事件

- `connect`: 连接成功
- `reconnect`: 重连中
- `disconnect`: 断开连接
- `offline`: 离线
- `close`: 连接关闭
- `error`: 错误
- `message`: 收到消息

## 许可证

MIT
