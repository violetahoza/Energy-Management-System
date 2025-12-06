import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class WebSocketService {
    constructor() {
        this.client = null;
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
    }

    connect(userId, token, isAdmin = false) {
        if (this.client && this.connected && this.userId === userId) {
            console.log('✅ WebSocket already connected for user:', userId);
            return;
        }

        if (this.client) {
            this.disconnect();
        }

        this.userId = userId;
        this.token = token;
        this.isAdmin = isAdmin;

        console.log(`🔌 Initializing WebSocket connection for User ${userId}...`);

        const socket = new SockJS('http://localhost/ws');

        this.client = new Client({
            webSocketFactory: () => socket,
            connectHeaders: {
                'Authorization': `Bearer ${token}`
            },
            debug: (str) => console.log('STOMP: ' + str),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,

            onConnect: () => {
                console.log('✅ WebSocket Connected Successfully');
                this.connected = true;

                this.client.subscribe(`/user/queue/messages`, (message) => {
                    const parsed = JSON.parse(message.body);
                    this.notifySubscribers('messages', parsed);
                });

                this.client.subscribe(`/user/queue/alerts`, (message) => {
                    console.log('🚨 RAW Alert Received:', message.body);
                    const parsed = JSON.parse(message.body);
                    this.notifySubscribers('alerts', parsed);
                });

                if (isAdmin) {
                    this.client.subscribe(`/topic/admin-chat`, (message) => {
                        const parsed = JSON.parse(message.body);
                        this.notifySubscribers('admin-messages', parsed);
                    });
                }

                this.notifySubscribers('connect', true);
            },

            onDisconnect: () => {
                console.log('❌ WebSocket Disconnected');
                this.connected = false;
                this.notifySubscribers('disconnect', false);
            },

            onStompError: (frame) => {
                console.error('❌ STOMP error:', frame);
                this.notifySubscribers('error', frame);
            }
        });

        this.client.activate();
    }

    disconnect() {
        if (this.client) {
            this.client.deactivate();
            this.client = null;
            this.connected = false;
        }
    }

    subscribe(eventType, callback) {
        if (this.subscribers[eventType]) {
            this.subscribers[eventType].push(callback);
            if (eventType === 'connect' && this.connected) {
                callback(true);
            }
        }
    }

    unsubscribe(eventType, callback) {
        if (this.subscribers[eventType]) {
            this.subscribers[eventType] = this.subscribers[eventType].filter(cb => cb !== callback);
        }
    }

    notifySubscribers(eventType, data) {
        if (this.subscribers[eventType]) {
            this.subscribers[eventType].forEach(callback => callback(data));
        }
    }

    sendMessage(content) {
        if (this.client && this.connected) {
            this.client.publish({
                destination: '/app/chat.sendMessage',
                body: JSON.stringify({ content })
            });
        }
    }

    sendAdminResponse(userId, content) {
        if (this.client && this.connected) {
            this.client.publish({
                destination: '/app/chat.adminResponse',
                body: JSON.stringify({ userId, content })
            });
        }
    }
}

export default WebSocketService;