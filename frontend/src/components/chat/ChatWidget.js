import React, { useState, useEffect, useRef } from 'react';
import websocketService from '../../services/websocket';
import { useAuth } from '../../context/AuthContext';
import '../../styles/App.css';

const ChatWidget = () => {
    const { user } = useAuth();
    const [isOpen, setIsOpen] = useState(false);
    const [messages, setMessages] = useState([]);
    const [inputMessage, setInputMessage] = useState('');
    const [isConnected, setIsConnected] = useState(false);
    const messagesEndRef = useRef(null);

    useEffect(() => {
        if (user) {
            const token = localStorage.getItem('token');

            const handleMessage = (message) => {
                console.log('ChatWidget received message:', message);
                setMessages(prev => [...prev, message]);
            };

            const handleConnect = () => {
                setIsConnected(true);
                console.log('ChatWidget: Connected');
            };

            const handleDisconnect = () => {
                setIsConnected(false);
                console.log('ChatWidget: Disconnected');
            };

            websocketService.subscribe('messages', handleMessage);
            websocketService.subscribe('connect', handleConnect);
            websocketService.subscribe('disconnect', handleDisconnect);

            if (!websocketService.isConnected()) {
                console.log('ChatWidget: Connecting WebSocket for user:', user.userId);
                websocketService.connect(user.userId, token, false); // Not admin
            } else {
                console.log('ChatWidget: WebSocket already connected');
                setIsConnected(true);
            }

            return () => {
                websocketService.unsubscribe('messages', handleMessage);
                websocketService.unsubscribe('connect', handleConnect);
                websocketService.unsubscribe('disconnect', handleDisconnect);
            };
        }
    }, [user]);

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    const handleSendMessage = (e) => {
        e.preventDefault();
        if (inputMessage.trim() && isConnected) {
            // Send to server
            websocketService.sendMessage(inputMessage);
            setInputMessage('');
        }
    };

    const getMessageClassName = (type) => {
        switch (type) {
            case 'USER_MESSAGE':
                return 'message-user';
            case 'ADMIN_MESSAGE':
                return 'message-admin';
            case 'RULE_RESPONSE':
                return 'message-system rule-based';
            case 'AI_RESPONSE':
                return 'message-system ai-based';
            default:
                return 'message-system';
        }
    };

    return (
        <>
            <div className={`chat-widget ${isOpen ? 'open' : ''}`}>
                <div className="chat-header" onClick={() => setIsOpen(!isOpen)}>
                    <span>💬 Customer Support</span>
                    <span className="chat-status">
                        {isConnected ? '🟢' : '🔴'}
                    </span>
                </div>

                {isOpen && (
                    <div className="chat-body">
                        <div className="chat-messages">
                            {messages.length === 0 && (
                                <div className="chat-welcome">
                                    <p>👋 Welcome! How can I help you today?</p>
                                </div>
                            )}
                            {messages.map((msg, index) => (
                                <div key={index} className={`message ${getMessageClassName(msg.type)}`}>
                                    <div className="message-sender">{msg.senderName}</div>
                                    <div className="message-content">{msg.content}</div>
                                    <div className="message-time">
                                        {new Date(msg.timestamp).toLocaleTimeString()}
                                    </div>
                                    {msg.type === 'RULE_RESPONSE' && (
                                        <div className="message-badge">🤖 Rule-Based</div>
                                    )}
                                    {msg.type === 'AI_RESPONSE' && (
                                        <div className="message-badge">🧠 AI-Powered</div>
                                    )}
                                </div>
                            ))}
                            <div ref={messagesEndRef} />
                        </div>

                        <form className="chat-input-form" onSubmit={handleSendMessage}>
                            <input
                                type="text"
                                value={inputMessage}
                                onChange={(e) => setInputMessage(e.target.value)}
                                placeholder="Type your message..."
                                className="chat-input"
                                disabled={!isConnected}
                            />
                            <button
                                type="submit"
                                className="chat-send-btn"
                                disabled={!isConnected || !inputMessage.trim()}
                            >
                                Send
                            </button>
                        </form>
                    </div>
                )}
            </div>

            {!isOpen && (
                <button className="chat-toggle-btn" onClick={() => setIsOpen(true)}>
                    💬
                </button>
            )}
        </>
    );
};

export default ChatWidget;