<template>
  <view class="container">
    <view class="title">MQTT 连接测试</view>
    
    <view class="section">
      <text class="label">Broker URL:</text>
      <input class="input" v-model="brokerUrl" placeholder="mqtt://broker.emqx.io:1883" />
    </view>
    
    <view class="section">
      <text class="label">Client ID:</text>
      <input class="input" v-model="clientId" placeholder="client_id" />
    </view>
    
    <view class="section">
      <text class="label">用户名 (可选):</text>
      <input class="input" v-model="username" placeholder="username" />
    </view>
    
    <view class="section">
      <text class="label">密码 (可选):</text>
      <input class="input" v-model="password" type="password" placeholder="password" />
    </view>
    
    <view class="section">
      <view class="checkbox-row">
        <checkbox-group @change="handleCleanSessionChange">
          <label>
            <checkbox value="clean" :checked="cleanSession" />
            <text class="checkbox-label">Clean Session (清除会话)</text>
          </label>
        </checkbox-group>
      </view>
    </view>
    
    <view class="button-group">
      <button @click="handleConnect" :disabled="connected">连接</button>
      <button @click="handleDisconnect" :disabled="!connected">断开</button>
    </view>
    
    <view class="status">
      <text>状态: {{ connectionStatus }}</text>
    </view>
    
    <view class="section" v-if="connected">
      <text class="label">订阅主题:</text>
      <input class="input" v-model="subscribeTopic" placeholder="test/topic" />
      <button @click="handleSubscribe">订阅</button>
    </view>
    
    <view class="section" v-if="connected">
      <text class="label">发布主题:</text>
      <input class="input" v-model="publishTopic" placeholder="test/topic" />
      <text class="label">消息:</text>
      <textarea class="textarea" v-model="publishMessage" placeholder="Hello MQTT" />
      <button @click="handlePublish">发布</button>
    </view>
    
    <view class="log-section">
      <text class="log-title">日志:</text>
      <scroll-view class="log-content" scroll-y>
        <text class="log-item" v-for="(log, index) in logs" :key="index">{{ log }}</text>
      </scroll-view>
    </view>
  </view>
</template>

<script>
import { connect, disconnect, subscribe, publish, on } from '@/uni_modules/changbi-mqtt'

export default {
  data() {
    return {
      brokerUrl: 'mqtt://broker.emqx.io:1883',
      clientId: 'client_' + Date.now(),
      username: '',
      password: '',
      cleanSession: true,
      subscribeTopic: 'test/topic',
      publishTopic: 'test/topic',
      publishMessage: 'Hello MQTT',
      connected: false,
      connectionStatus: '未连接',
      logs: []
    }
  },
  
  onLoad() {
    this.setupEventListeners()
  },
  
  onUnload() {
    if (this.connected) {
      disconnect()
    }
  },
  
  methods: {
    setupEventListeners() {
      on('connect', (data) => {
        console.log('[MQTT] Connected to MQTT broker')
        this.addLog('✅ 连接成功')
        this.connected = true
        this.connectionStatus = '已连接'
      })
      
      on('error', (data) => {
        const errorMsg = data && data.error ? JSON.stringify(data.error) : '未知错误'
        this.addLog('❌ 错误: ' + errorMsg)
        this.connectionStatus = '错误'
      })
      
      on('close', (data) => {
        this.addLog('🔌 连接已关闭')
        this.connected = false
        this.connectionStatus = '已断开'
      })
      
      on('message', (data) => {
        const topic = data && data.topic ? data.topic : '未知主题'
        const payload = data && data.payload ? data.payload : ''
        this.addLog(`📨 收到消息 [${topic}]: ${payload}`)
      })
    },
    
    handleCleanSessionChange(e) {
      this.cleanSession = e.detail.value.includes('clean')
      this.addLog(`Clean Session 设置为: ${this.cleanSession}`)
    },
    
    async handleConnect() {
      try {
        this.addLog('正在连接...')
        this.connectionStatus = '连接中'
        
        const options = {
          url: this.brokerUrl,
          clientId: this.clientId,
          clean: this.cleanSession,
          keepalive: 60,
          connectTimeout: 30000
        }
        
        // 如果提供了用户名和密码，添加到连接选项
        if (this.username) {
          options.username = this.username
        }
        if (this.password) {
          options.password = this.password
        }
        
        this.addLog(`连接参数 - Clean Session: ${this.cleanSession}`)
        
        await connect(options)
      } catch (error) {
        this.addLog('❌ 连接失败: ' + error.message)
        this.connectionStatus = '连接失败'
      }
    },
    
    async handleDisconnect() {
      try {
        this.addLog('正在断开连接...')
        await disconnect()
      } catch (error) {
        this.addLog('❌ 断开失败: ' + error.message)
      }
    },
    
    async handleSubscribe() {
      try {
        this.addLog(`订阅主题: ${this.subscribeTopic}`)
        await subscribe(this.subscribeTopic, 0)
        this.addLog('✅ 订阅成功')
      } catch (error) {
        this.addLog('❌ 订阅失败: ' + error.message)
      }
    },
    
    async handlePublish() {
      try {
        this.addLog(`发布消息到 ${this.publishTopic}: ${this.publishMessage}`)
        await publish(this.publishTopic, this.publishMessage, { qos: 0, retain: false })
        this.addLog('✅ 发布成功')
      } catch (error) {
        this.addLog('❌ 发布失败: ' + error.message)
      }
    },
    
    addLog(message) {
      const timestamp = new Date().toLocaleTimeString()
      this.logs.unshift(`[${timestamp}] ${message}`)
      if (this.logs.length > 100) {
        this.logs.pop()
      }
    }
  }
}
</script>

<style scoped>
.container {
  padding: 20px;
}

.title {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 20px;
  text-align: center;
}

.section {
  margin-bottom: 15px;
}

.label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}

.input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-bottom: 10px;
}

.textarea {
  width: 100%;
  height: 80px;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-bottom: 10px;
}

.checkbox-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.checkbox-label {
  font-size: 14px;
}

.button-group {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

button {
  flex: 1;
  padding: 12px;
  background-color: #007aff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
}

button:disabled {
  background-color: #ccc;
}

.status {
  padding: 10px;
  background-color: #f5f5f5;
  border-radius: 4px;
  margin-bottom: 15px;
  text-align: center;
  font-weight: bold;
}

.log-section {
  margin-top: 20px;
}

.log-title {
  font-weight: bold;
  margin-bottom: 10px;
  display: block;
}

.log-content {
  height: 300px;
  background-color: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 10px;
}

.log-item {
  display: block;
  margin-bottom: 5px;
  font-size: 12px;
  font-family: monospace;
}
</style>
