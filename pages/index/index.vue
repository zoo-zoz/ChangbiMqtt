<template>
  <view class="container">
    <view class="header">
      <text class="title">MQTT 测试</text>
      <view class="status-badge" :class="statusClass">
        {{ connectionStatus }}
        <text v-if="reconnecting" class="reconnect-info"> ({{ reconnectAttempts }}/{{ maxReconnectAttempts }})</text>
      </view>
    </view>
    
    <!-- 连接配置 -->
    <view class="card">
      <view class="card-title">连接配置</view>
      
      <view class="form-row">
        <text class="label">协议</text>
        <radio-group @change="handleProtocolChange" class="radio-group-inline">
          <label class="radio-label">
            <radio value="tcp" :checked="protocol === 'tcp'" />
            <text>TCP</text>
          </label>
          <label class="radio-label">
            <radio value="ws" :checked="protocol === 'ws'" />
            <text>WS</text>
          </label>
          <label class="radio-label">
            <radio value="wss" :checked="protocol === 'wss'" />
            <text>WSS</text>
          </label>
        </radio-group>
      </view>
      
      <view class="form-row">
        <text class="label">服务器</text>
        <input class="input-compact" v-model="brokerUrl" />
      </view>
      
      <view class="form-row-inline">
        <view class="form-col">
          <text class="label-small">Client ID</text>
          <input class="input-compact" v-model="clientId" />
        </view>
        <view class="form-col">
          <text class="label-small">用户名</text>
          <input class="input-compact" v-model="username" placeholder="可选" />
        </view>
      </view>
      
      <view class="form-row-inline">
        <view class="form-col">
          <text class="label-small">密码</text>
          <input class="input-compact" v-model="password" type="password" placeholder="可选" />
        </view>
        <view class="form-col">
          <text class="label-small">重连间隔(ms)</text>
          <input class="input-compact" v-model.number="reconnectPeriod" type="number" />
        </view>
      </view>
      
      <view class="form-row-inline">
        <view class="form-col">
          <text class="label-small">最大重连次数</text>
          <input class="input-compact" v-model.number="maxReconnectAttempts" type="number" />
        </view>
        <view class="form-col">
          <text class="label-small">保活间隔(s)</text>
          <input class="input-compact" v-model.number="keepalive" type="number" />
        </view>
      </view>
      
      <view class="form-row">
        <checkbox-group @change="handleCleanSessionChange">
          <label class="checkbox-inline">
            <checkbox value="clean" :checked="cleanSession" />
            <text>Clean Session</text>
          </label>
        </checkbox-group>
      </view>
      
      <view class="button-row">
        <button @click="handleConnect" :disabled="connected || reconnecting" class="btn btn-primary">连接</button>
        <button @click="handleDisconnect" :disabled="!connected && !reconnecting" class="btn btn-danger">断开</button>
      </view>
    </view>
    
    <!-- 订阅发布 -->
    <view class="card" v-if="connected">
      <view class="card-title">订阅 / 发布</view>
      
      <view class="tabs">
        <view class="tab" :class="{ active: activeTab === 'subscribe' }" @click="activeTab = 'subscribe'">
          <text>订阅</text>
        </view>
        <view class="tab" :class="{ active: activeTab === 'publish' }" @click="activeTab = 'publish'">
          <text>发布</text>
        </view>
      </view>
      
      <!-- 订阅标签页 -->
      <view v-if="activeTab === 'subscribe'" class="tab-content">
        <view class="input-group">
          <input class="input-flex" v-model="subscribeTopic" placeholder="主题，如: test/topic" />
          <picker @change="handleSubQosChange" :value="subQosIndex" :range="['QoS 0', 'QoS 1', 'QoS 2']" class="picker-inline">
            <view class="picker-compact">{{ ['QoS 0', 'QoS 1', 'QoS 2'][subQosIndex] }}</view>
          </picker>
          <button @click="handleSubscribe" class="btn-small">订阅</button>
        </view>
        
        <view class="topic-list" v-if="subscribedTopics.length > 0">
          <text class="list-title">已订阅主题 ({{ subscribedTopics.length }})</text>
          <view class="topic-item" v-for="(item, index) in subscribedTopics" :key="index">
            <view class="topic-info">
              <text class="topic-text">{{ item.topic }}</text>
              <text class="topic-qos">QoS {{ item.qos }}</text>
            </view>
            <text @click="handleUnsubscribe(item.topic)" class="topic-remove">×</text>
          </view>
        </view>
        <view v-else class="empty-hint">
          <text>暂无订阅</text>
        </view>
      </view>
      
      <!-- 发布标签页 -->
      <view v-if="activeTab === 'publish'" class="tab-content">
        <view class="form-row">
          <text class="label-small">主题</text>
          <input class="input-compact" v-model="publishTopic" placeholder="test/topic" />
        </view>
        
        <view class="form-row">
          <text class="label-small">消息内容</text>
          <textarea class="textarea-compact" v-model="publishMessage" placeholder="Hello MQTT!" />
        </view>
        
        <view class="publish-options">
          <view class="option-item">
            <text class="label-small">QoS</text>
            <picker @change="handlePubQosChange" :value="pubQosIndex" :range="['0', '1', '2']">
              <view class="picker-compact">{{ pubQosIndex }}</view>
            </picker>
          </view>
          
          <view class="option-item">
            <checkbox-group @change="handleRetainChange">
              <label class="checkbox-inline">
                <checkbox value="retain" :checked="retain" />
                <text>Retain (保留消息)</text>
              </label>
            </checkbox-group>
          </view>
        </view>
        
        <button @click="handlePublish" class="btn btn-primary btn-block">发布消息</button>
      </view>
    </view>
    
    <!-- 统计信息 -->
    <view class="card" v-if="connected || reconnecting">
      <view class="card-title">统计信息</view>
      <view class="stats-grid">
        <view class="stat-item">
          <text class="stat-value">{{ stats.sent }}</text>
          <text class="stat-label">已发送</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ stats.received }}</text>
          <text class="stat-label">已接收</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ subscribedTopics.length }}</text>
          <text class="stat-label">订阅数</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ totalReconnects }}</text>
          <text class="stat-label">重连次数</text>
        </view>
      </view>
    </view>
    
    <!-- 连接状态详情 -->
    <view class="card" v-if="connected || reconnecting">
      <view class="card-title">连接状态</view>
      <view class="state-info">
        <view class="state-row">
          <text class="state-label">状态:</text>
          <text class="state-value" :class="'state-' + currentState.status">{{ currentState.status }}</text>
        </view>
        <view class="state-row" v-if="currentState.connectedAt">
          <text class="state-label">连接时间:</text>
          <text class="state-value">{{ formatTime(currentState.connectedAt) }}</text>
        </view>
        <view class="state-row" v-if="reconnecting">
          <text class="state-label">重连进度:</text>
          <text class="state-value">{{ reconnectAttempts }} / {{ maxReconnectAttempts }}</text>
        </view>
        <view class="state-row" v-if="currentState.sessionPresent">
          <text class="state-label">会话保持:</text>
          <text class="state-value state-success">是</text>
        </view>
      </view>
    </view>
    
    <!-- 日志 -->
    <view class="card">
      <view class="card-header">
        <text class="card-title">日志</text>
        <text @click="clearLogs" class="link-text">清空</text>
      </view>
      
      <scroll-view class="log-view" scroll-y>
        <view class="log-item" v-for="(log, index) in logs" :key="index" :class="'log-' + log.type">
          <text class="log-time">{{ log.time }}</text>
          <text class="log-msg">{{ log.msg }}</text>
        </view>
        <view v-if="logs.length === 0" class="log-empty">暂无日志</view>
      </scroll-view>
    </view>
  </view>
</template>

<script>
import { connect, disconnect, subscribe, unsubscribe, publish, on, getConnectionState } from '@/uni_modules/changbi-mqtt'

export default {
  data() {
    return {
      protocol: 'ws',
      brokerUrl: 'ws://broker.emqx.io:8083/mqtt',
      clientId: 'changbi_' + Date.now(),
      username: '',
      password: '',
      cleanSession: true,
      reconnectPeriod: 5000,
      maxReconnectAttempts: 10,
      keepalive: 60,
      connected: false,
      reconnecting: false,
      reconnectAttempts: 0,
      totalReconnects: 0,
      connectionStatus: '未连接',
      
      currentState: {
        status: 'disconnected',
        connectedAt: null,
        sessionPresent: false
      },
      
      activeTab: 'subscribe',
      subscribeTopic: 'test/changbi',
      subQosIndex: 0,
      subscribedTopics: [],
      
      publishTopic: 'test/changbi',
      publishMessage: 'Hello MQTT!',
      pubQosIndex: 0,
      retain: false,
      
      stats: {
        sent: 0,
        received: 0
      },
      
      logs: []
    }
  },
  
  computed: {
    statusClass() {
      if (this.connected) return 'status-connected'
      if (this.reconnecting) return 'status-reconnecting'
      if (this.connectionStatus === '连接中') return 'status-connecting'
      if (this.connectionStatus.includes('失败') || this.connectionStatus.includes('离线')) return 'status-error'
      return 'status-disconnected'
    }
  },
  
  onLoad() {
    this.setupEventListeners()
    this.log('info', '📱 应用已启动，默认使用 EMQX WebSocket 服务器')
  },
  
  onUnload() {
    if (this.connected) {
      disconnect()
    }
  },
  
  methods: {
    setupEventListeners() {
      on('connect', (data) => {
        this.log('success', '✅ 连接成功')
        this.connected = true
        this.reconnecting = false
        this.reconnectAttempts = 0
        this.connectionStatus = '已连接'
        
        // 更新状态信息
        this.updateConnectionState()
        
        if (data && data.sessionPresent) {
          this.log('info', '📋 会话已保持')
        }
      })
      
      on('reconnect', (data) => {
        this.reconnecting = true
        this.connected = false
        this.reconnectAttempts = data && data.reconnectAttempts ? data.reconnectAttempts : 0
        this.totalReconnects++
        this.connectionStatus = '重连中'
        this.log('warning', `🔄 正在重连... (尝试 ${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
        
        // 更新状态信息
        this.updateConnectionState()
      })
      
      on('offline', (data) => {
        this.reconnecting = false
        this.connected = false
        this.connectionStatus = '已离线'
        const msg = data && data.error ? data.error : '达到最大重连次数'
        this.log('error', '❌ 已离线: ' + msg)
        
        // 更新状态信息
        this.updateConnectionState()
        
        uni.showModal({
          title: '连接失败',
          content: '无法连接到服务器，已达到最大重连次数',
          showCancel: false
        })
      })
      
      on('error', (data) => {
        const msg = data && data.error ? data.error : '未知错误'
        // 如果错误信息包含换行符，分多行显示
        if (typeof msg === 'string' && msg.includes('\n')) {
          const lines = msg.split('\n')
          lines.forEach((line, index) => {
            if (index === 0) {
              this.log('error', '❌ ' + line)
            } else if (line.trim()) {
              this.log('error', line)
            }
          })
        } else {
          this.log('error', '❌ 错误: ' + (typeof msg === 'string' ? msg : JSON.stringify(msg)))
        }
      })
      
      on('close', () => {
        this.log('warning', '🔌 连接已关闭')
        if (!this.reconnecting) {
          this.connected = false
          this.connectionStatus = '已断开'
          this.subscribedTopics = []
          this.stats = { sent: 0, received: 0 }
          this.reconnectAttempts = 0
        }
        
        // 更新状态信息
        this.updateConnectionState()
      })
      
      on('message', (data) => {
        const topic = data && data.topic ? data.topic : '?'
        const payload = data && data.payload ? data.payload : ''
        const qos = data && data.qos !== undefined ? data.qos : 0
        const retain = data && data.retain ? ' [Retain]' : ''
        this.stats.received++
        this.log('message', `📨 [${topic}] QoS${qos}${retain}: ${payload}`)
      })
    },
    
    updateConnectionState() {
      try {
        const state = getConnectionState()
        if (state) {
          this.currentState = {
            status: state.status || 'unknown',
            connectedAt: state.connectedAt || null,
            sessionPresent: state.sessionPresent || false
          }
        }
      } catch (e) {
        console.error('获取连接状态失败:', e)
      }
    },
    
    formatTime(timestamp) {
      if (!timestamp) return '-'
      const date = new Date(timestamp)
      return date.toLocaleTimeString('zh-CN', { hour12: false })
    },
    
    handleProtocolChange(e) {
      this.protocol = e.detail.value
      const urls = {
        tcp: 'mqtt://broker.emqx.io:1883',
        ws: 'ws://broker.emqx.io:8083/mqtt',
        wss: 'wss://broker.emqx.io:8084/mqtt'
      }
      this.brokerUrl = urls[this.protocol]
      this.log('info', '🔄 切换到 ' + this.protocol.toUpperCase() + ' 协议')
    },
    
    handleCleanSessionChange(e) {
      this.cleanSession = e.detail.value.includes('clean')
      this.log('info', '🔧 Clean Session: ' + (this.cleanSession ? '开启' : '关闭'))
    },
    
    async handleConnect() {
      try {
        this.log('info', '🔄 正在连接...')
        this.log('info', '📡 服务器: ' + this.brokerUrl)
        this.log('info', '🆔 Client ID: ' + this.clientId)
        this.log('info', '🔄 重连间隔: ' + this.reconnectPeriod + 'ms')
        this.log('info', '🔢 最大重连次数: ' + this.maxReconnectAttempts)
        this.log('info', '💓 保活间隔: ' + this.keepalive + 's')
        this.connectionStatus = '连接中'
        
        const options = {
          url: this.brokerUrl,
          clientId: this.clientId,
          clean: this.cleanSession,
          keepalive: this.keepalive,
          connectTimeout: 30000,
          reconnectPeriod: this.reconnectPeriod,
          maxReconnectAttempts: this.maxReconnectAttempts
        }
        
        if (this.username) {
          options.username = this.username
          this.log('info', '👤 用户名: ' + this.username)
        }
        if (this.password) {
          options.password = this.password
          this.log('info', '🔑 已设置密码')
        }
        
        await connect(options)
      } catch (error) {
        this.log('error', '❌ 连接失败: ' + error.message)
        this.connectionStatus = '连接失败'
        this.connected = false
        this.reconnecting = false
      }
    },
    
    async handleDisconnect() {
      try {
        this.log('info', '🔄 正在断开连接...')
        this.reconnecting = false
        this.reconnectAttempts = 0
        await disconnect()
      } catch (error) {
        this.log('error', '❌ 断开失败: ' + error.message)
      }
    },
    
    handleSubQosChange(e) {
      this.subQosIndex = e.detail.value
    },
    
    async handleSubscribe() {
      if (!this.subscribeTopic) {
        this.log('warning', '⚠️ 请输入订阅主题')
        return
      }
      
      // 检查是否已订阅
      const exists = this.subscribedTopics.find(item => item.topic === this.subscribeTopic)
      if (exists) {
        this.log('warning', '⚠️ 已订阅该主题')
        return
      }
      
      try {
        const qos = this.subQosIndex
        this.log('info', `🔄 订阅主题: ${this.subscribeTopic} (QoS ${qos})`)
        await subscribe(this.subscribeTopic, qos)
        
        this.subscribedTopics.push({
          topic: this.subscribeTopic,
          qos: qos
        })
        
        this.log('success', '✅ 订阅成功')
      } catch (error) {
        this.log('error', '❌ 订阅失败: ' + error.message)
      }
    },
    
    async handleUnsubscribe(topic) {
      try {
        this.log('info', '🔄 取消订阅: ' + topic)
        await unsubscribe(topic)
        
        const index = this.subscribedTopics.findIndex(item => item.topic === topic)
        if (index > -1) {
          this.subscribedTopics.splice(index, 1)
        }
        
        this.log('success', '✅ 取消订阅成功')
      } catch (error) {
        this.log('error', '❌ 取消订阅失败: ' + error.message)
      }
    },
    
    handlePubQosChange(e) {
      this.pubQosIndex = e.detail.value
    },
    
    handleRetainChange(e) {
      this.retain = e.detail.value.includes('retain')
      this.log('info', '🔧 Retain: ' + (this.retain ? '开启' : '关闭'))
    },
    
    async handlePublish() {
      if (!this.publishTopic) {
        this.log('warning', '⚠️ 请输入发布主题')
        return
      }
      
      try {
        const qos = this.pubQosIndex
        const retainText = this.retain ? ' [Retain]' : ''
        this.log('info', `🔄 发布到 [${this.publishTopic}] QoS${qos}${retainText}`)
        this.log('info', `📝 内容: ${this.publishMessage}`)
        
        await publish(this.publishTopic, this.publishMessage, { 
          qos: qos, 
          retain: this.retain 
        })
        
        this.stats.sent++
        this.log('success', '✅ 发布成功')
      } catch (error) {
        this.log('error', '❌ 发布失败: ' + error.message)
      }
    },
    
    log(type, msg) {
      const time = new Date().toLocaleTimeString('zh-CN', { hour12: false })
      this.logs.unshift({ type, time, msg })
      if (this.logs.length > 100) {
        this.logs.pop()
      }
    },
    
    clearLogs() {
      this.logs = []
      this.log('info', '🗑️ 日志已清空')
    }
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 15px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.title {
  font-size: 22px;
  font-weight: bold;
  color: #333;
}

.status-badge {
  padding: 6px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.status-disconnected {
  background: #e0e0e0;
  color: #666;
}

.status-connecting {
  background: #fff3cd;
  color: #856404;
}

.status-reconnecting {
  background: #ffeaa7;
  color: #d63031;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.status-connected {
  background: #d4edda;
  color: #155724;
}

.status-error {
  background: #f8d7da;
  color: #721c24;
}

.reconnect-info {
  font-size: 10px;
  margin-left: 4px;
}

.card {
  background: white;
  border-radius: 12px;
  padding: 15px;
  margin-bottom: 15px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.link-text {
  color: #007aff;
  font-size: 14px;
}

.form-row {
  margin-bottom: 12px;
}

.form-row-inline {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.form-col {
  flex: 1;
}

.label {
  display: block;
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
  font-weight: 500;
}

.label-small {
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 6px;
}

.input-compact {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  background: #fafafa;
}

.input-flex {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  background: #fafafa;
}

.textarea-compact {
  width: 100%;
  height: 80px;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  background: #fafafa;
}

.radio-group-inline {
  display: flex;
  gap: 15px;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
}

.checkbox-inline {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
}

.button-row {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

.btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
}

.btn-primary {
  background: #007aff;
  color: white;
}

.btn-danger {
  background: #ff3b30;
  color: white;
}

.btn-block {
  width: 100%;
}

.btn:disabled {
  background: #ccc;
  color: #999;
}

.tabs {
  display: flex;
  border-bottom: 1px solid #e0e0e0;
  margin-bottom: 15px;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 10px;
  color: #999;
  font-size: 14px;
  font-weight: 500;
}

.tab.active {
  color: #007aff;
  border-bottom: 2px solid #007aff;
}

.tab-content {
  min-height: 150px;
}

.input-group {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  align-items: center;
}

.picker-inline {
  flex-shrink: 0;
}

.btn-small {
  padding: 10px 15px;
  background: #34c759;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.list-title {
  display: block;
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.topic-list {
  margin-top: 12px;
}

.topic-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  margin-bottom: 8px;
}

.topic-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.topic-text {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}

.topic-qos {
  font-size: 11px;
  color: #999;
}

.topic-remove {
  font-size: 24px;
  color: #ff3b30;
  font-weight: bold;
  padding: 0 8px;
}

.empty-hint {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 13px;
}

.publish-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.picker-compact {
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 13px;
  color: #333;
  min-width: 60px;
  text-align: center;
}

.stats-grid {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.stat-item {
  flex: 1;
  min-width: 70px;
  text-align: center;
  padding: 10px 8px;
  background: #f8f9fa;
  border-radius: 8px;
}

.stat-value {
  display: block;
  font-size: 20px;
  font-weight: bold;
  color: #007aff;
  margin-bottom: 4px;
}

.stat-label {
  display: block;
  font-size: 11px;
  color: #999;
}

.state-info {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 12px;
}

.state-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px solid #e0e0e0;
}

.state-row:last-child {
  border-bottom: none;
}

.state-label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.state-value {
  font-size: 13px;
  color: #333;
  font-weight: 600;
}

.state-connected {
  color: #28a745;
}

.state-reconnecting {
  color: #ffc107;
}

.state-disconnected {
  color: #6c757d;
}

.state-success {
  color: #28a745;
}

.log-view {
  height: 300px;
  background: #1e1e1e;
  border-radius: 8px;
  padding: 10px;
}

.log-item {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
  font-family: 'Courier New', monospace;
  line-height: 1.4;
}

.log-time {
  color: #858585;
  flex-shrink: 0;
}

.log-msg {
  color: #d4d4d4;
  word-break: break-word;
  white-space: pre-wrap;
  flex: 1;
}

.log-success .log-msg {
  color: #4ec9b0;
}

.log-error .log-msg {
  color: #f48771;
}

.log-warning .log-msg {
  color: #dcdcaa;
}

.log-message .log-msg {
  color: #9cdcfe;
}

.log-empty {
  text-align: center;
  color: #666;
  padding: 20px;
  font-size: 14px;
}
</style>
