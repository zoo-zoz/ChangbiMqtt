# Changbi MQTT 插件 API 文档

## 概述

Changbi MQTT 是一个用于 UniApp 的 MQTT 客户端 UTS 插件，提供 Android 平台的 MQTT 连接功能。

## 安装

将插件复制到项目的 `uni_modules` 目录下。

## 导入

```typescript
import * as mqtt from '@/uni_modules/changbi-mqtt'
```

## API 参考

### 连接管理

#### connect(options: ConnectionOptions): Promise<void>

连接到 MQTT Broker。

**参数：**
```typescript
interface ConnectionOptions {
  url: string                    // MQTT Broker 地址
  clientId: string               // 客户端 ID
  username?: string              // 用户名（可选）
  password?: string              // 密码（可选）
  clean?: boolean                // 清除会话标志（默认 true）
  connectTimeout?: number        // 连接超时时间（毫秒，默认 30000）
  keepalive?: number             // 心跳间隔（秒，默认 60）
  reconnect?: boolean            // 是否自动重连（默认 true）
  reconnectPeriod?: number       // 重连间隔（毫秒，默认 1000）
  maxReconnectAttempts?: number  // 最大重连次数（默认 无限）
  ssl?: boolean                  // 是否使用 SSL/TLS
  certPath?: string              // 证书路径（自签名证书）
  allowUntrusted?: boolean       // 是否允许不受信任的证书
  will?: WillMessage             // Will 消息
}
```

**示例：**
```typescript
await mqtt.connect({
  url: 'mqtt://broker.emqx.io:1883',
  clientId: 'uniapp_client_001',
  username: 'user',
  password: 'pass',
  clean: true,
  connectTimeout: 30000,
  keepalive: 60
})
```

#### disconnect(): Promise<void>

断开 MQTT 连接。

**示例：**
```typescript
await mqtt.disconnect()
```

#### isConnected(): boolean

检查是否已连接。

**返回值：** `boolean` - 是否已连接

**示例：**
```typescript
const connected = mqtt.isConnected()
console.log('Connected:', connected)
```

### 订阅管理

#### subscribe(topic: string | string[], qos?: QoS): Promise<void>

订阅一个或多个主题。

**参数：**
- `topic`: 主题字符串或主题数组
- `qos`: 服务质量等级（0, 1, 2），默认 0

**示例：**
```typescript
// 订阅单个主题
await mqtt.subscribe('test/topic', 0)

// 订阅多个主题
await mqtt.subscribe(['test/topic1', 'test/topic2'], 1)

// 使用通配符
await mqtt.subscribe('test/+/sensor', 0)  // 单层通配符
await mqtt.subscribe('test/#', 0)         // 多层通配符
```

#### unsubscribe(topic: string | string[]): Promise<void>

取消订阅一个或多个主题。

**参数：**
- `topic`: 主题字符串或主题数组

**示例：**
```typescript
// 取消订阅单个主题
await mqtt.unsubscribe('test/topic')

// 取消订阅多个主题
await mqtt.unsubscribe(['test/topic1', 'test/topic2'])
```

#### getSubscriptions(): Subscription[]

获取所有订阅。

**返回值：** `Subscription[]` - 订阅列表

```typescript
interface Subscription {
  topic: string
  qos: QoS
  subscribedAt: number
}
```

**示例：**
```typescript
const subscriptions = mqtt.getSubscriptions()
console.log('Subscriptions:', subscriptions)
```

#### getSubscriptionCount(): number

获取订阅数量。

**返回值：** `number` - 订阅数量

**示例：**
```typescript
const count = mqtt.getSubscriptionCount()
console.log('Subscription count:', count)
```

#### hasSubscription(topic: string): boolean

检查是否已订阅某个主题。

**参数：**
- `topic`: 主题字符串

**返回值：** `boolean` - 是否已订阅

**示例：**
```typescript
const hasSubscription = mqtt.hasSubscription('test/topic')
console.log('Has subscription:', hasSubscription)
```

### 消息发布

#### publish(topic: string, message: string | ArrayBuffer, options?: PublishOptions): Promise<void>

发布消息到指定主题。

**参数：**
- `topic`: 主题字符串
- `message`: 消息内容（字符串或二进制数据）
- `options`: 发布选项（可选）

```typescript
interface PublishOptions {
  qos?: QoS          // 服务质量等级（默认 0）
  retain?: boolean   // 保留消息标志（默认 false）
}
```

**示例：**
```typescript
// 发布字符串消息
await mqtt.publish('test/topic', 'Hello MQTT')

// 发布带 QoS 的消息
await mqtt.publish('test/topic', 'Hello MQTT', { qos: 1 })

// 发布保留消息
await mqtt.publish('test/topic', 'Hello MQTT', { qos: 1, retain: true })
```

### 事件监听

#### on(event: MQTTEvent, callback: EventCallback): void

注册事件监听器。

**参数：**
- `event`: 事件类型
- `callback`: 回调函数

**事件类型：**
- `connect` - 连接成功
- `reconnect` - 重连中
- `disconnect` - 断开连接
- `offline` - 离线
- `close` - 连接关闭
- `error` - 错误
- `message` - 收到消息

**示例：**
```typescript
// 连接成功
mqtt.on('connect', (data) => {
  console.log('Connected:', data.sessionPresent)
})

// 收到消息
mqtt.on('message', (data) => {
  console.log('Topic:', data.topic)
  console.log('Payload:', data.payload)
  console.log('QoS:', data.qos)
  console.log('Retain:', data.retain)
})

// 错误
mqtt.on('error', (data) => {
  console.error('Error:', data.error)
})

// 连接关闭
mqtt.on('close', () => {
  console.log('Connection closed')
})
```

#### off(event: MQTTEvent, callback?: EventCallback): void

移除事件监听器。

**参数：**
- `event`: 事件类型
- `callback`: 回调函数（可选，不传则移除该事件的所有监听器）

**示例：**
```typescript
// 移除特定回调
mqtt.off('message', messageHandler)

// 移除所有 message 事件监听器
mqtt.off('message')
```

### 状态查询

#### getConnectionState(): ConnectionState

获取连接状态。

**返回值：** `ConnectionState` - 连接状态对象

```typescript
interface ConnectionState {
  status: 'disconnected' | 'connecting' | 'connected' | 'reconnecting' | 'disconnecting'
  connectedAt?: number
  lastError?: ErrorInfo
  reconnectAttempts: number
  sessionPresent: boolean
}
```

**示例：**
```typescript
const state = mqtt.getConnectionState()
console.log('Status:', state.status)
console.log('Connected at:', state.connectedAt)
```

### 消息队列管理

#### getMessageQueueStats(): object

获取消息队列统计信息。

**返回值：** 包含队列统计的对象

```typescript
{
  pendingCount: number      // 待发送消息数量
  receivedCount: number     // 已接收消息数量
  maxQueueSize: number      // 最大队列大小
}
```

**示例：**
```typescript
const stats = mqtt.getMessageQueueStats()
console.log('Pending:', stats.pendingCount)
console.log('Received:', stats.receivedCount)
console.log('Max queue size:', stats.maxQueueSize)
```

#### clearMessageQueues(): void

清空消息队列。

**示例：**
```typescript
mqtt.clearMessageQueues()
```

### 日志管理

#### setLogLevel(level: LogLevel): void

设置日志级别。

**参数：**
- `level`: 日志级别

```typescript
enum LogLevel {
  DEBUG = 0,
  INFO = 1,
  WARN = 2,
  ERROR = 3,
  NONE = 4
}
```

**示例：**
```typescript
mqtt.setLogLevel(mqtt.LogLevel.DEBUG)
```

## 完整示例

```typescript
import * as mqtt from '@/uni_modules/changbi-mqtt'

export default {
  data() {
    return {
      isConnected: false,
      messages: []
    }
  },
  
  onLoad() {
    this.initMqtt()
  },
  
  onUnload() {
    mqtt.disconnect()
    mqtt.off('connect')
    mqtt.off('message')
    mqtt.off('error')
  },
  
  methods: {
    initMqtt() {
      // 注册事件监听
      mqtt.on('connect', (data) => {
        console.log('Connected')
        this.isConnected = true
        
        // 连接成功后订阅主题
        mqtt.subscribe('test/topic', 0)
      })
      
      mqtt.on('message', (data) => {
        console.log('Message:', data.topic, data.payload)
        this.messages.push({
          topic: data.topic,
          payload: data.payload,
          qos: data.qos
        })
      })
      
      mqtt.on('error', (data) => {
        console.error('Error:', data.error)
      })
      
      mqtt.on('close', () => {
        console.log('Connection closed')
        this.isConnected = false
      })
    },
    
    async connect() {
      try {
        await mqtt.connect({
          url: 'mqtt://broker.emqx.io:1883',
          clientId: 'uniapp_' + Date.now(),
          clean: true,
          connectTimeout: 30000,
          keepalive: 60
        })
      } catch (e) {
        console.error('Connect error:', e)
      }
    },
    
    async disconnect() {
      try {
        await mqtt.disconnect()
      } catch (e) {
        console.error('Disconnect error:', e)
      }
    },
    
    async subscribe() {
      try {
        await mqtt.subscribe('test/topic', 0)
        console.log('Subscribed')
      } catch (e) {
        console.error('Subscribe error:', e)
      }
    },
    
    async publish() {
      try {
        await mqtt.publish('test/topic', 'Hello MQTT', { qos: 0 })
        console.log('Published')
      } catch (e) {
        console.error('Publish error:', e)
      }
    }
  }
}
```

## QoS 等级说明

### QoS 0 - 最多一次
- 消息可能丢失
- 不需要确认
- 最快的传输方式
- 适用于不重要的数据

### QoS 1 - 至少一次
- 保证消息到达
- 可能重复接收
- 需要确认
- 适用于重要但可容忍重复的数据

### QoS 2 - 恰好一次
- 保证消息到达且不重复
- 最慢但最可靠
- 需要四次握手
- 适用于关键数据

## 通配符说明

### 单层通配符 (+)
匹配单个层级的任意内容。

**示例：**
- `test/+/sensor` 匹配 `test/room1/sensor`、`test/room2/sensor`
- 不匹配 `test/room1/data/sensor`

### 多层通配符 (#)
匹配多个层级的任意内容，必须在主题末尾。

**示例：**
- `test/#` 匹配 `test/room1`、`test/room1/sensor`、`test/room1/data/sensor`

## 注意事项

1. 在使用前必须先调用 `connect()` 建立连接
2. 订阅和发布操作需要在连接成功后进行
3. 使用 `@UTSJS.keepAlive` 装饰器确保回调函数可以持续触发
4. 消息队列有大小限制（默认 1000 条），超出会自动移除最旧的消息
5. 断开连接时会自动清空订阅列表（clean session = true）
6. 建议在页面卸载时调用 `disconnect()` 和 `off()` 清理资源

## 常见问题

### Q: 为什么只能收到第一条消息？
A: 确保使用了 `@UTSJS.keepAlive` 装饰器，详见 CALLBACK_FIX.md。

### Q: 如何实现自动重连？
A: 目前版本暂不支持自动重连，将在后续版本中实现。

### Q: 支持 SSL/TLS 吗？
A: 目前版本暂不支持 SSL/TLS，将在后续版本中实现。

### Q: 如何处理大量消息？
A: 插件内置消息队列，可以处理高频消息。如需更高性能，建议使用批量处理。

## 更新日志

详见 [changelog.md](./changelog.md)

## 许可证

MIT
