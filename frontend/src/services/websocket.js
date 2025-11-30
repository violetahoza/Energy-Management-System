import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class WebSocketService {
    constructor() {
        this.client = null;
        this.connected = false;
    }

    connect(userId, token, callbacks = {}) {
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

                // Subscribe to chat messages
                this.client.subscribe(`/user/queue/messages`, (message) => {
                    console.log('📨 Received chat message:', message.body);
                    const chatMessage = JSON.parse(message.body);
                    callbacks.onMessage && callbacks.onMessage(chatMessage);
                });
                console.log(`✓ Subscribed to: /user/queue/messages`);

                this.client.subscribe(`/user/queue/alerts`, (message) => {
                    console.log('🚨 Received alert (user queue):', message.body);
                    const alert = JSON.parse(message.body);
                    callbacks.onAlert && callbacks.onAlert(alert);
                });
                console.log(`✓ Subscribed to: /user/queue/alerts`);

                this.client.subscribe(`/topic/alerts/${userId}`, (message) => {
                    console.log('🚨 Received alert (topic):', message.body);
                    const alert = JSON.parse(message.body);
                    callbacks.onAlert && callbacks.onAlert(alert);
                });
                console.log(`✓ Subscribed to: /topic/alerts/${userId}`);

                callbacks.onConnect && callbacks.onConnect();
                console.log('=== All Subscriptions Active ===');
            },
            onDisconnect: () => {
                console.log('❌ WebSocket Disconnected');
                this.connected = false;
                callbacks.onDisconnect && callbacks.onDisconnect();
            },
            onStompError: (frame) => {
                console.error('❌ STOMP error:', frame);
                callbacks.onError && callbacks.onError(frame);
            }
        });

        this.client.activate();
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
            console.log('🔌 WebSocket disconnected');
        }
    }
}

export default new WebSocketService();