<template>
	<view class="container">
		<view class="header">
			<text class="title">MQTT 插件测试</text>
			<text class="status" :class="statusClass">{{ statusText }}</text>
		</view>
		
		<!-- 连接配置 -->
		<view class="section">
			<text class="section-title">连接配置</text>
			<view class="input-group">
				<text class="label">Broker URL:</text>
				<input class="input" v-model="brokerUrl" placeholder="mqtt://broker.emqx.io:1883" />
			</view>
			<view class="input-group">
				<text class="label">Client ID:</text>
				<input class="input" v-model="clientId" placeholder="自动生成" />
			</view>
			<view class="button-group">
				<button class="btn btn-primary" @click="handleConnect" :disabled="isConnected">连接</button>
				<button class="btn btn-danger" @click="handleDisconnect" :disabled="!isConnected">断开</button>
			</view>
		</view>
		
		<!-- 订阅管理 -->
		<view class="section">
			<text class="section-title">订阅管理 ({{ subscriptionCount }})</text>
			<view class="input-group">
				<text class="label">主题:</text>
				<input class="input" v-model="subscribeTopic" placeholder="test/topic" />
			</view>
			<view class="input-group">
				<text class="label">QoS:</text>
				<picker class="picker" mode="selector" :range="qosOptions" :value="subscribeQos" @change="onQosChange">
					<view class="picker-text">QoS {{ subscribeQos }}</view>
				</picker>
			</view>
			<view class="button-group">
				<button class="btn btn-success" @click="handleSubscribe" :disabled="!isConnected">订阅</button>
				<button class="btn btn-warning" @click="handleUnsubscribe" :disabled="!isConnected">取消订阅</button>
			</view>
			<view class="subscriptions">
				<text class="sub-title">当前订阅:</text>
				<view v-for="(sub, index) in subscriptions" :key="index" class="subscription-item">
					<text class="sub-topic">{{ sub.topic }}</text>
					<text class="sub-qos">QoS {{ sub.qos }}</text>
				</view>
			</view>
		</view>
		
		<!-- 消息发布 -->
		<view class="section">
			<text class="section-title">消息发布</text>
			<view class="input-group">
				<text class="label">主题:</text>
				<input class="input" v-model="publishTopic" placeholder="test/topic" />
			</view>
			<view class="input-group">
				<text class="label">消息:</text>
				<textarea class="textarea" v-model="publishMessage" placeholder="输入消息内容" />
			</view>
			<view class="input-group">
				<text class="label">QoS:</text>
				<picker class="picker" mode="selector" :range="qosOptions" :value="publishQos" @change="onPublishQosChange">
					<view class="picker-text">QoS {{ publishQos }}</view>
				</picker>
			</view>
			<view class="checkbox-group">
				<checkbox :checked="publishRetain" @change="onRetainChange" />
				<text class="checkbox-label">保留消息</text>
			</view>
			<button class="btn btn-primary btn-block" @click="handlePublish" :disabled="!isConnected">发布消息</button>
		</view>
		
		<!-- 消息日志 -->
		<view class="section">
			<text class="section-title">消息日志 ({{ messages.length }})</text>
			<view class="button-group">
				<button class="btn btn-secondary btn-sm" @click="clearMessages">清空日志</button>
				<button class="btn btn-secondary btn-sm" @click="showQueueStats">队列统计</button>
			</view>
			<scroll-view class="message-list" scroll-y>
				<view v-for="(msg, index) in messages" :key="index" class="message-item">
					<text class="msg-time">{{ msg.time }}</text>
					<text class="msg-topic">{{ msg.topic }}</text>
					<text class="msg-payload">{{ msg.payload }}</text>
					<text class="msg-qos">QoS {{ msg.qos }}</text>
				</view>
				<view v-if="messages.length === 0" class="empty-message">
					<text>暂无消息</text>
				</view>
			</scroll-view>
		</view>
	</view>
</template>

<script>
	import * as mqtt from '@/uni_modules/changbi-mqtt'
	
	export default {
		data() {
			return {
				// 连接状态
				isConnected: false,
				statusText: '未连接',
				statusClass: 'status-disconnected',
				
				// 连接配置
				brokerUrl: 'mqtt://broker.emqx.io:1883',
				clientId: '',
				
				// 订阅配置
				subscribeTopic: 'test/topic',
				subscribeQos: 0,
				qosOptions: [0, 1, 2],
				subscriptions: [],
				subscriptionCount: 0,
				
				// 发布配置
				publishTopic: 'test/topic',
				publishMessage: 'Hello MQTT',
				publishQos: 0,
				publishRetain: false,
				
				// 消息列表
				messages: []
			}
		},
		onLoad() {
			// 生成随机 Client ID
			this.clientId = 'uniapp_' + Math.random().toString(36).substring(2, 15)
			
			// 注册事件监听
			this.registerEventListeners()
		},
		onUnload() {
			// 移除事件监听
			mqtt.off('connect')
			mqtt.off('message')
			mqtt.off('close')
			mqtt.off('error')
		},
		methods: {
			// 注册事件监听器
			registerEventListeners() {
				// 连接成功
				mqtt.on('connect', (data) => {
					console.log('Connected:', data)
					this.isConnected = true
					this.statusText = '已连接'
					this.statusClass = 'status-connected'
					this.updateSubscriptions()
					uni.showToast({
						title: '连接成功',
						icon: 'success'
					})
				})
				
				// 收到消息
				mqtt.on('message', (data) => {
					console.log('Message received:', data)
					this.messages.unshift({
						time: this.formatTime(new Date()),
						topic: data.topic || '',
						payload: data.payload || '',
						qos: data.qos || 0
					})
					// 限制消息列表长度
					if (this.messages.length > 100) {
						this.messages.pop()
					}
				})
				
				// 连接关闭
				mqtt.on('close', () => {
					console.log('Connection closed')
					this.isConnected = false
					this.statusText = '已断开'
					this.statusClass = 'status-disconnected'
					this.subscriptions = []
					this.subscriptionCount = 0
					uni.showToast({
						title: '连接已断开',
						icon: 'none'
					})
				})
				
				// 错误
				mqtt.on('error', (data) => {
					console.error('MQTT Error:', data)
					uni.showToast({
						title: '错误: ' + (data.error?.message || '未知错误'),
						icon: 'none',
						duration: 3000
					})
				})
			},
			
			// 连接
			async handleConnect() {
				try {
					this.statusText = '连接中...'
					this.statusClass = 'status-connecting'
					
					await mqtt.connect({
						url: this.brokerUrl,
						clientId: this.clientId,
						clean: true,
						connectTimeout: 30000,
						keepalive: 60
					})
				} catch (e) {
					console.error('Connect error:', e)
					this.statusText = '连接失败'
					this.statusClass = 'status-disconnected'
					uni.showToast({
						title: '连接失败: ' + e.message,
						icon: 'none',
						duration: 3000
					})
				}
			},
			
			// 断开连接
			async handleDisconnect() {
				try {
					await mqtt.disconnect()
				} catch (e) {
					console.error('Disconnect error:', e)
					uni.showToast({
						title: '断开失败: ' + e.message,
						icon: 'none'
					})
				}
			},
			
			// 订阅
			async handleSubscribe() {
				if (!this.subscribeTopic) {
					uni.showToast({
						title: '请输入主题',
						icon: 'none'
					})
					return
				}
				
				try {
					// 检查是否已订阅
					if (mqtt.hasSubscription(this.subscribeTopic)) {
						uni.showToast({
							title: '已订阅该主题',
							icon: 'none'
						})
						return
					}
					
					await mqtt.subscribe(this.subscribeTopic, this.subscribeQos)
					this.updateSubscriptions()
					uni.showToast({
						title: '订阅成功',
						icon: 'success'
					})
				} catch (e) {
					console.error('Subscribe error:', e)
					uni.showToast({
						title: '订阅失败: ' + e.message,
						icon: 'none'
					})
				}
			},
			
			// 取消订阅
			async handleUnsubscribe() {
				if (!this.subscribeTopic) {
					uni.showToast({
						title: '请输入主题',
						icon: 'none'
					})
					return
				}
				
				try {
					await mqtt.unsubscribe(this.subscribeTopic)
					this.updateSubscriptions()
					uni.showToast({
						title: '取消订阅成功',
						icon: 'success'
					})
				} catch (e) {
					console.error('Unsubscribe error:', e)
					uni.showToast({
						title: '取消订阅失败: ' + e.message,
						icon: 'none'
					})
				}
			},
			
			// 发布消息
			async handlePublish() {
				if (!this.publishTopic) {
					uni.showToast({
						title: '请输入主题',
						icon: 'none'
					})
					return
				}
				
				if (!this.publishMessage) {
					uni.showToast({
						title: '请输入消息内容',
						icon: 'none'
					})
					return
				}
				
				try {
					await mqtt.publish(this.publishTopic, this.publishMessage, {
						qos: this.publishQos,
						retain: this.publishRetain
					})
					uni.showToast({
						title: '发布成功',
						icon: 'success'
					})
				} catch (e) {
					console.error('Publish error:', e)
					uni.showToast({
						title: '发布失败: ' + e.message,
						icon: 'none'
					})
				}
			},
			
			// 更新订阅列表
			updateSubscriptions() {
				this.subscriptions = mqtt.getSubscriptions()
				this.subscriptionCount = mqtt.getSubscriptionCount()
			},
			
			// 清空消息
			clearMessages() {
				this.messages = []
				mqtt.clearMessageQueues()
				uni.showToast({
					title: '已清空',
					icon: 'success'
				})
			},
			
			// 显示队列统计
			showQueueStats() {
				const stats = mqtt.getMessageQueueStats()
				uni.showModal({
					title: '消息队列统计',
					content: `待发送: ${stats.pendingCount}\n已接收: ${stats.receivedCount}\n最大队列: ${stats.maxQueueSize}`,
					showCancel: false
				})
			},
			
			// QoS 选择
			onQosChange(e) {
				this.subscribeQos = this.qosOptions[e.detail.value]
			},
			
			onPublishQosChange(e) {
				this.publishQos = this.qosOptions[e.detail.value]
			},
			
			// Retain 选择
			onRetainChange(e) {
				this.publishRetain = e.detail.value
			},
			
			// 格式化时间
			formatTime(date) {
				const h = date.getHours().toString().padStart(2, '0')
				const m = date.getMinutes().toString().padStart(2, '0')
				const s = date.getSeconds().toString().padStart(2, '0')
				return `${h}:${m}:${s}`
			}
		}
	}
</script>

<style>
	.container {
		padding: 20rpx;
		background-color: #f5f5f5;
	}
	
	.header {
		background-color: #fff;
		padding: 30rpx;
		margin-bottom: 20rpx;
		border-radius: 10rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
	}
	
	.title {
		font-size: 40rpx;
		font-weight: bold;
		color: #333;
		margin-bottom: 20rpx;
	}
	
	.status {
		font-size: 28rpx;
		padding: 10rpx 30rpx;
		border-radius: 20rpx;
	}
	
	.status-disconnected {
		background-color: #f0f0f0;
		color: #999;
	}
	
	.status-connecting {
		background-color: #fff3cd;
		color: #856404;
	}
	
	.status-connected {
		background-color: #d4edda;
		color: #155724;
	}
	
	.section {
		background-color: #fff;
		padding: 30rpx;
		margin-bottom: 20rpx;
		border-radius: 10rpx;
	}
	
	.section-title {
		font-size: 32rpx;
		font-weight: bold;
		color: #333;
		margin-bottom: 20rpx;
		display: block;
	}
	
	.input-group {
		margin-bottom: 20rpx;
	}
	
	.label {
		font-size: 28rpx;
		color: #666;
		display: block;
		margin-bottom: 10rpx;
	}
	
	.input {
		width: 100%;
		height: 70rpx;
		padding: 0 20rpx;
		border: 1rpx solid #ddd;
		border-radius: 5rpx;
		font-size: 28rpx;
	}
	
	.textarea {
		width: 100%;
		min-height: 150rpx;
		padding: 20rpx;
		border: 1rpx solid #ddd;
		border-radius: 5rpx;
		font-size: 28rpx;
	}
	
	.picker {
		width: 100%;
		height: 70rpx;
		padding: 0 20rpx;
		border: 1rpx solid #ddd;
		border-radius: 5rpx;
		display: flex;
		align-items: center;
	}
	
	.picker-text {
		font-size: 28rpx;
		color: #333;
	}
	
	.checkbox-group {
		display: flex;
		align-items: center;
		margin-bottom: 20rpx;
	}
	
	.checkbox-label {
		font-size: 28rpx;
		color: #666;
		margin-left: 10rpx;
	}
	
	.button-group {
		display: flex;
		gap: 20rpx;
		margin-top: 20rpx;
	}
	
	.btn {
		flex: 1;
		height: 70rpx;
		line-height: 70rpx;
		font-size: 28rpx;
		border-radius: 5rpx;
		border: none;
	}
	
	.btn-block {
		width: 100%;
	}
	
	.btn-sm {
		height: 60rpx;
		line-height: 60rpx;
		font-size: 24rpx;
	}
	
	.btn-primary {
		background-color: #007aff;
		color: #fff;
	}
	
	.btn-success {
		background-color: #28a745;
		color: #fff;
	}
	
	.btn-warning {
		background-color: #ffc107;
		color: #333;
	}
	
	.btn-danger {
		background-color: #dc3545;
		color: #fff;
	}
	
	.btn-secondary {
		background-color: #6c757d;
		color: #fff;
	}
	
	.btn[disabled] {
		opacity: 0.5;
	}
	
	.subscriptions {
		margin-top: 20rpx;
		padding-top: 20rpx;
		border-top: 1rpx solid #eee;
	}
	
	.sub-title {
		font-size: 28rpx;
		color: #666;
		display: block;
		margin-bottom: 10rpx;
	}
	
	.subscription-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 15rpx 20rpx;
		background-color: #f8f9fa;
		border-radius: 5rpx;
		margin-bottom: 10rpx;
	}
	
	.sub-topic {
		font-size: 26rpx;
		color: #333;
		flex: 1;
	}
	
	.sub-qos {
		font-size: 24rpx;
		color: #666;
		padding: 5rpx 15rpx;
		background-color: #e9ecef;
		border-radius: 10rpx;
	}
	
	.message-list {
		max-height: 600rpx;
		margin-top: 20rpx;
	}
	
	.message-item {
		padding: 20rpx;
		background-color: #f8f9fa;
		border-radius: 5rpx;
		margin-bottom: 10rpx;
	}
	
	.msg-time {
		font-size: 24rpx;
		color: #999;
		display: block;
		margin-bottom: 5rpx;
	}
	
	.msg-topic {
		font-size: 26rpx;
		color: #007aff;
		display: block;
		margin-bottom: 5rpx;
	}
	
	.msg-payload {
		font-size: 28rpx;
		color: #333;
		display: block;
		margin-bottom: 5rpx;
		word-break: break-all;
	}
	
	.msg-qos {
		font-size: 24rpx;
		color: #666;
		display: inline-block;
		padding: 5rpx 15rpx;
		background-color: #e9ecef;
		border-radius: 10rpx;
	}
	
	.empty-message {
		text-align: center;
		padding: 60rpx 0;
		color: #999;
		font-size: 28rpx;
	}
</style>
