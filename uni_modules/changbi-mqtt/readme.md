# Changbi MQTT 插件

## 简介

Changbi MQTT 是一个用于 UniApp 的 MQTT 客户端 UTS 插件，提供稳定、高性能的 MQTT 连接功能，支持 Android 平台。

## 版本

**当前版本**: v1.1.2  
**更新日期**: 2024-04-12


## 平台支持

- Android ✅
- iOS ❌（暂不支持）

## 安装

将插件导入到您的 UniApp 项目的 `uni_modules` 目录下。

## 快速开始

```javascript
import * as mqtt from '@/uni_modules/changbi-mqtt'

// 1. 注册事件监听
mqtt.on('connect', (data) => {
  console.log('连接成功')
  // 订阅主题
  mqtt.subscribe('test/topic', 0)
})

mqtt.on('message', (data) => {
  console.log('收到消息:', data.topic, data.payload)
})

// 2. 连接到 Broker
await mqtt.connect({
  url: 'mqtt://broker.emqx.io:1883',
  clientId: 'uniapp_' + Date.now(),
  clean: true,
  keepalive: 60
})

// 3. 发布消息
await mqtt.publish('test/topic', 'Hello MQTT', { qos: 0 })

// 4. 断开连接
await mqtt.disconnect()
```

## 核心功能

### 订阅管理

```javascript
// 订阅主题
await mqtt.subscribe('test/topic', 0)

// 订阅多个主题
await mqtt.subscribe(['topic1', 'topic2'], 1)

// 使用通配符
await mqtt.subscribe('test/+/sensor', 0)  // 单层通配符
await mqtt.subscribe('test/#', 0)         // 多层通配符

// 检查是否已订阅
if (mqtt.hasSubscription('test/topic')) {
  console.log('Already subscribed')
}

// 获取所有订阅
const subscriptions = mqtt.getSubscriptions()
console.log('Subscriptions:', subscriptions)

// 获取订阅数量
const count = mqtt.getSubscriptionCount()
console.log('Count:', count)

// 取消订阅
await mqtt.unsubscribe('test/topic')
```

### 消息发布

```javascript
// 发布字符串消息
await mqtt.publish('test/topic', 'Hello MQTT')

// 发布带 QoS 的消息
await mqtt.publish('test/topic', 'Hello', { qos: 1 })

// 发布保留消息
await mqtt.publish('test/topic', 'Hello', { qos: 1, retain: true })
```

### 消息队列

```javascript
// 获取队列统计
const stats = mqtt.getMessageQueueStats()
console.log('Pending:', stats.pendingCount)
console.log('Received:', stats.receivedCount)
console.log('Max queue size:', stats.maxQueueSize)

// 清空消息队列
mqtt.clearMessageQueues()
```

## API 文档

详细的 API 文档请参考 [API.md](./API.md)

### 主要方法

| 方法 | 说明 | 返回值 |
|------|------|--------|
| `connect(options)` | 连接到 MQTT Broker | Promise<void> |
| `disconnect()` | 断开连接 | Promise<void> |
| `subscribe(topic, qos)` | 订阅主题 | Promise<void> |
| `unsubscribe(topic)` | 取消订阅 | Promise<void> |
| `publish(topic, message, options)` | 发布消息 | Promise<void> |
| `on(event, callback)` | 注册事件监听 | void |
| `off(event, callback)` | 移除事件监听 | void |
| `isConnected()` | 检查连接状态 | boolean |
| `getSubscriptions()` | 获取所有订阅 | Subscription[] |
| `getSubscriptionCount()` | 获取订阅数量 | number |
| `hasSubscription(topic)` | 检查是否已订阅 | boolean |
| `getMessageQueueStats()` | 获取队列统计 | object |
| `clearMessageQueues()` | 清空消息队列 | void |

### 事件类型

| 事件 | 说明 | 数据 |
|------|------|------|
| `connect` | 连接成功 | `{ sessionPresent: boolean }` |
| `message` | 收到消息 | `{ topic, payload, qos, retain }` |
| `error` | 错误 | `{ error: ErrorInfo }` |
| `close` | 连接关闭 | `{}` |
| `reconnect` | 重连中 | `{ attempt: number }` |
| `disconnect` | 断开连接 | `{}` |
| `offline` | 离线 | `{}` |

## 配置选项

```typescript
interface ConnectionOptions {
  url: string                    // MQTT Broker 地址
  clientId: string               // 客户端 ID
  username?: string              // 用户名
  password?: string              // 密码
  clean?: boolean                // 清除会话（默认 true）
  connectTimeout?: number        // 连接超时（默认 30000ms）
  keepalive?: number             // 心跳间隔（默认 60s）
  reconnect?: boolean            // 自动重连（默认 true）
  reconnectPeriod?: number       // 重连间隔（默认 1000ms）
  maxReconnectAttempts?: number  // 最大重连次数
}
```

## 测试

项目包含完整的测试页面和测试指南：

1. 运行测试应用到 Android 设备
2. 参考 `TEST_GUIDE.md` 进行测试
3. 使用 MQTTX 等工具进行对比测试

推荐测试 Broker：
- EMQX: `mqtt://broker.emqx.io:1883`
- Mosquitto: `mqtt://test.mosquitto.org:1883`
- HiveMQ: `mqtt://broker.hivemq.com:1883`

## 示例项目

项目本身就是一个完整的测试应用，包含：
- 连接管理界面
- 订阅管理界面
- 消息发布界面
- 消息日志显示
- 队列统计功能

## 更新日志

详细更新日志请查看 [changelog.md](./changelog.md)

## 技术支持
如有问题或建议，请提交 Issue 或 Pull Request。

## 许可证

MIT

---

**开发状态**: 🚧 活跃开发中  
**稳定性**: ⚠️ Beta 版本，建议在生产环境使用前充分测试