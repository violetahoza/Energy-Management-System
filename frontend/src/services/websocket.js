import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class WebSocketService {
    constructor() {
        this.client = null;
        this.connected = false;
    }

    connect(userId, token, callbacks = {}) {
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
                console.log('WebSocket Connected');
                this.connected = true;

                // Subscribe to chat messages
                this.client.subscribe(`/user/queue/messages`, (message) => {
                    const chatMessage = JSON.parse(message.body);
                    callbacks.onMessage && callbacks.onMessage(chatMessage);
                });

                // Subscribe to overconsumption alerts
                this.client.subscribe(`/user/queue/alerts`, (message) => {
                    const alert = JSON.parse(message.body);
                    callbacks.onAlert && callbacks.onAlert(alert);
                });

                // Subscribe to system notifications
                this.client.subscribe('/topic/notifications', (message) => {
                    const notification = JSON.parse(message.body);
                    callbacks.onNotification && callbacks.onNotification(notification);
                });

                callbacks.onConnect && callbacks.onConnect();
            },
            onDisconnect: () => {
                console.log('WebSocket Disconnected');
                this.connected = false;
                callbacks.onDisconnect && callbacks.onDisconnect();
            },
            onStompError: (frame) => {
                console.error('STOMP error:', frame);
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
    }

    disconnect() {
        if (this.client) {
            this.client.deactivate();
            this.connected = false;
        }
    }
}

export default new WebSocketService();