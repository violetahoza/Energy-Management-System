import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class WebSocketService {
    constructor() {
        this.client = null;
        this.connected = false;
        this.subscribers = {
            alerts: [],
            messages: [],
            connect: [],
            disconnect: [],
            error: []
        };
    }

    connect(userId, token) {
        if (this.client && this.connected) {
            console.log('⚠️ WebSocket already connected, skipping reconnect');
            return;
        }

        console.log('=== WebSocket Connection Initiated ===');
        console.log('User ID:', userId);

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
            onConnect: () => {
                console.log('✓ WebSocket Connected Successfully');
                this.connected = true;

                this.client.subscribe(`/user/queue/messages`, (message) => {
                    console.log('📨 Received chat message:', message.body);
                    const chatMessage = JSON.parse(message.body);
                    this.notifySubscribers('messages', chatMessage);
                });
                console.log(`✓ Subscribed to: /user/queue/messages`);

                this.client.subscribe(`/user/queue/alerts`, (message) => {
                    console.log('🚨 Received alert:', message.body);
                    const alert = JSON.parse(message.body);
                    this.notifySubscribers('alerts', alert);
                });
                console.log(`✓ Subscribed to: /user/queue/alerts`);

                console.log('=== All Subscriptions Active ===');
                this.notifySubscribers('connect');
            },
            onDisconnect: () => {
                console.log('❌ WebSocket Disconnected');
                this.connected = false;
                this.notifySubscribers('disconnect');
            },
            onStompError: (frame) => {
                console.error('❌ STOMP error:', frame);
                this.notifySubscribers('error', frame);
            }
        });

        this.client.activate();
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

    disconnect() {
        if (this.client) {
            this.client.deactivate();
            this.connected = false;
            this.subscribers = {
                alerts: [],
                messages: [],
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