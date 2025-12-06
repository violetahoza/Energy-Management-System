import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useWebSocket } from '../../context/WebSocketContext';
import '../../styles/App.css';

const ChatWidget = () => {
    const { isConnected, subscribe, unsubscribe, sendMessage } = useWebSocket();
    const { user } = useAuth();
    const [isOpen, setIsOpen] = useState(false);

    const [messages, setMessages] = useState(() => {
        if (user) {
            try {
                const stored = localStorage.getItem(`chat_messages_${user.userId}`);
                return stored ? JSON.parse(stored) : [];
            } catch (e) {
                console.error('Error loading messages:', e);
                return [];
            }
        }
        return [];
    });

    const [inputMessage, setInputMessage] = useState('');
    const messagesEndRef = useRef(null);

    useEffect(() => {
        if (user) {
            localStorage.setItem(`chat_messages_${user.userId}`, JSON.stringify(messages));
        }
    }, [messages, user]);

    useEffect(() => {
        if (user) {
            const token = localStorage.getItem('token');

            const handleMessage = (message) => {
                console.log('ChatWidget received message:', message);
                setMessages(prev => [...prev, message]);
            };

            subscribe('messages', handleMessage);

            return () => {
                unsubscribe('messages', handleMessage);
            };
        }
    }, [user, subscribe, unsubscribe]);

    useEffect(() => {
        scrollToBottom();
    }, [messages, isOpen]);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    const handleSendMessage = (e) => {
        e.preventDefault();
        if (inputMessage.trim() && isConnected) {
            const content = inputMessage.trim();

            const userMessage = {
                content: content,
                sender: user.userId,
                senderName: user.username || 'You',
                type: 'USER_MESSAGE',
                timestamp: Date.now()
            };

            setMessages(prev => [...prev, userMessage]);
            sendMessage(content);
            setInputMessage('');
        }
    };

    const clearChat = () => {
        if (window.confirm('Clear all chat messages?')) {
            setMessages([]);
            if (user) {
                localStorage.removeItem(`chat_messages_${user.userId}`);
            }
        }
    };

    const getMessageClassName = (type) => {
        switch (type) {
            case 'USER_MESSAGE': return 'message-user';
            case 'ADMIN_MESSAGE': return 'message-admin';
            case 'RULE_RESPONSE': return 'message-system rule-based';
            case 'AI_RESPONSE': return 'message-system ai-based';
            default: return 'message-system';
        }
    };

    if (!user) return null;

    return (
        <>
            <div className={`chat-widget ${isOpen ? 'open' : ''}`}>
                <div className="chat-header" onClick={() => setIsOpen(!isOpen)}>
                    <span>💬 Customer Support</span>
                    <span className="chat-status">{isConnected ? '🟢' : '🔴'}</span>
                </div>

                {isOpen && (
                    <div className="chat-body">
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px', borderBottom: '1px solid #e0e0e0' }}>
                            <span style={{ fontSize: '12px', color: '#666' }}>{isConnected ? '🟢 Connected' : '🔴 Disconnected'}</span>
                            <button onClick={clearChat} style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '12px', color: '#666' }}>🗑️ Clear</button>
                        </div>

                        <div className="chat-messages">
                            {messages.length === 0 && (<div className="chat-welcome"><p>👋 Welcome! How can I help you today?</p></div>)}
                            {messages.map((msg, index) => (
                                <div key={index} className={`message ${getMessageClassName(msg.type)}`}>
                                    <div className="message-sender">{msg.senderName}</div>
                                    <div className="message-content">{msg.content}</div>
                                    <div className="message-time">{new Date(msg.timestamp).toLocaleTimeString()}</div>
                                    {msg.type === 'RULE_RESPONSE' && <div className="message-badge">🤖 Rule-Based</div>}
                                    {msg.type === 'AI_RESPONSE' && <div className="message-badge">🧠 AI-Powered</div>}
                                </div>
                            ))}
                            <div ref={messagesEndRef} />
                        </div>

                        <form className="chat-input-form" onSubmit={handleSendMessage}>
                            <input
                                type="text"
                                value={inputMessage}
                                onChange={(e) => setInputMessage(e.target.value)}
                                placeholder={isConnected ? "Type your message..." : "Connecting..."}
                                className="chat-input"
                                disabled={!isConnected}
                            />
                            <button type="submit" className="chat-send-btn" disabled={!isConnected || !inputMessage.trim()}>Send</button>
                        </form>
                    </div>
                )}
            </div>
            {!isOpen && (<button className="chat-toggle-btn" onClick={() => setIsOpen(true)}>💬</button>)}
        </>
    );
};

export default ChatWidget;