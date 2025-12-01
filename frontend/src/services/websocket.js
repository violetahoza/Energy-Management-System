import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class WebSocketService {
    constructor() {
        this.client = null;
        this.connected = false;
        this.userId = null;
        this.token = null;
        this.isAdmin = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 2000;
        this.subscribers = {
            alerts: [],
            messages: [],
            'admin-messages': [],
            connect: [],
            disconnect: [],
            error: []
        };
    }

    connect(userId, token, isAdmin = false) {
        this.userId = userId;
        this.token = token;
        this.isAdmin = isAdmin;

        if (this.client && this.connected) {
            console.log('⚠️ WebSocket already connected, skipping reconnect');
            return Promise.resolve();
        }

        if (this.client) {
            console.log('⚠️ Cleaning up existing client before reconnect');
            try {
                this.client.deactivate();
            } catch (e) {
                console.log('Error deactivating client:', e);
            }
        }

        return new Promise((resolve, reject) => {
            console.log('=== WebSocket Connection Initiated ===');
            console.log('User ID:', userId);
            console.log('Is Admin:', isAdmin);

            const socket = new SockJS('http://localhost/ws');

            this.client = new Client({
                webSocketFactory: () => socket,
                connectHeaders: {
                    'Authorization': `Bearer ${token}`,
                    'X-User-Id': userId.toString()
                },
                debug: (str) => {
                    console.log('STOMP: ' + str);
                },
                reconnectDelay: this.reconnectDelay,
                heartbeatIncoming: 4000,
                heartbeatOutgoing: 4000,
                onConnect: () => {
                    console.log('✓ WebSocket Connected Successfully');
                    this.connected = true;
                    this.reconnectAttempts = 0;

                    // Subscribe to personal message queue (for both clients and admins)
                    this.client.subscribe(`/user/queue/messages`, (message) => {
                        console.log('📨 Received chat message:', message.body);
                        const chatMessage = JSON.parse(message.body);
                        this.notifySubscribers('messages', chatMessage);
                    });
                    console.log(`✓ Subscribed to: /user/queue/messages`);

                    // Subscribe to alerts queue
                    this.client.subscribe(`/user/queue/alerts`, (message) => {
                        console.log('🚨 Received alert:', message.body);
                        const alert = JSON.parse(message.body);
                        this.notifySubscribers('alerts', alert);
                    });
                    console.log(`✓ Subscribed to: /user/queue/alerts`);

                    if (isAdmin) {
                        this.client.subscribe(`/topic/admin-chat`, (message) => {
                            console.log('👤 Admin received user message:', message.body);
                            const userMessage = JSON.parse(message.body);
                            this.notifySubscribers('admin-messages', userMessage);
                        });
                        console.log(`✓ Subscribed to: /topic/admin-chat`);
                    }

                    console.log('=== All Subscriptions Active ===');
                    this.notifySubscribers('connect');
                    resolve();
                },
                onDisconnect: () => {
                    console.log('❌ WebSocket Disconnected');
                    this.connected = false;
                    this.notifySubscribers('disconnect');
                    this.attemptReconnect();
                },
                onStompError: (frame) => {
                    console.error('❌ STOMP error:', frame);
                    this.notifySubscribers('error', frame);
                    reject(frame);
                }
            });

            this.client.activate();
        });
    }

    attemptReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('Max reconnection attempts reached');
            return;
        }

        if (!this.userId || !this.token) {
            console.log('No stored credentials for reconnection');
            return;
        }

        this.reconnectAttempts++;
        console.log(`Attempting reconnection ${this.reconnectAttempts}/${this.maxReconnectAttempts}...`);

        setTimeout(() => {
            this.connect(this.userId, this.token, this.isAdmin)
                .catch(err => console.error('Reconnection failed:', err));
        }, this.reconnectDelay);
    }

    subscribe(eventType, callback) {
        if (this.subscribers[eventType]) {
            this.subscribers[eventType].push(callback);
            console.log(`📝 Added subscriber for ${eventType}. Total: ${this.subscribers[eventType].length}`);
        }
    }

    unsubscribe(eventType, callback) {
        if (this.subscribers[eventType]) {
            const index = this.subscribers[eventType].indexOf(callback);
            if (index > -1) {
                this.subscribers[eventType].splice(index, 1);
                console.log(`🗑️ Removed subscriber for ${eventType}. Total: ${this.subscribers[eventType].length}`);
            }
        }
    }

    notifySubscribers(eventType, data) {
        if (this.subscribers[eventType]) {
            this.subscribers[eventType].forEach(callback => {
                try {
                    callback(data);
                } catch (error) {
                    console.error(`Error in ${eventType} subscriber:`, error);
                }
            });
        }
    }

    sendMessage(content) {
        if (!this.connected) {
            console.error('WebSocket not connected');
            return;
        }

        this.client.publish({
            destination: '/app/chat.sendMessage',
            body: JSON.stringify({ content })
        });
        console.log('📤 Sent message:', content);
    }

    sendAdminResponse(userId, content) {
        if (!this.connected) {
            console.error('WebSocket not connected');
            return;
        }

        this.client.publish({
            destination: '/app/chat.adminResponse',
            body: JSON.stringify({ userId, content })
        });
        console.log('📤 Admin sent message to user:', userId);
    }

    disconnect() {
        if (this.client) {
            this.client.deactivate();
            this.connected = false;
            this.userId = null;
            this.token = null;
            this.isAdmin = false;
            this.subscribers = {
                alerts: [],
                messages: [],
                'admin-messages': [],
                connect: [],
                disconnect: [],
                error: []
            };
            console.log('🔌 WebSocket disconnected and subscribers cleared');
        }
    }

    isConnected() {
        return this.connected;
    }
}

export default new WebSocketService();