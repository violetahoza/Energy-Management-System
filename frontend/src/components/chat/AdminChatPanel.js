import React, { useState, useEffect, useRef } from 'react';
import websocketService from '../../services/websocket';
import { useAuth } from '../../context/AuthContext';
import '../../styles/AdminChat.css';

const AdminChatPanel = () => {
    const { user } = useAuth();
    const [activeSessions, setActiveSessions] = useState(new Map());
    const [selectedUserId, setSelectedUserId] = useState(null);
    const [inputMessage, setInputMessage] = useState('');
    const [isConnected, setIsConnected] = useState(false);
    const messagesEndRef = useRef(null);

    useEffect(() => {
        if (user && user.role === 'ADMIN') {
            const token = localStorage.getItem('token');

            const handleAdminMessage = (message) => {
                console.log('Admin received user message:', message);

                setActiveSessions(prev => {
                    const newSessions = new Map(prev);
                    const userId = message.sender;
                    const userMessages = newSessions.get(userId) || [];
                    newSessions.set(userId, [...userMessages, message]);
                    return newSessions;
                });
            };

            const handleConnect = () => {
                setIsConnected(true);
                console.log('Admin Chat: Connected');
            };

            const handleDisconnect = () => {
                setIsConnected(false);
                console.log('Admin Chat: Disconnected');
            };

            // Subscribe to admin chat topic
            websocketService.subscribe('admin-messages', handleAdminMessage);
            websocketService.subscribe('connect', handleConnect);
            websocketService.subscribe('disconnect', handleDisconnect);

            if (!websocketService.isConnected()) {
                console.log('Admin Chat: Connecting WebSocket');
                websocketService.connect(user.userId, token);
            } else {
                setIsConnected(true);
            }

            return () => {
                websocketService.unsubscribe('admin-messages', handleAdminMessage);
                websocketService.unsubscribe('connect', handleConnect);
                websocketService.unsubscribe('disconnect', handleDisconnect);
            };
        }
    }, [user]);

    useEffect(() => {
        scrollToBottom();
    }, [selectedUserId, activeSessions]);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    const handleSendMessage = (e) => {
        e.preventDefault();
        if (inputMessage.trim() && selectedUserId && isConnected) {
            websocketService.sendAdminResponse(selectedUserId, inputMessage);

            // Add admin message to the session
            const adminMessage = {
                content: inputMessage,
                sender: 'ADMIN',
                senderName: 'Administrator',
                type: 'ADMIN_MESSAGE',
                timestamp: Date.now()
            };

            setActiveSessions(prev => {
                const newSessions = new Map(prev);
                const messages = newSessions.get(selectedUserId) || [];
                newSessions.set(selectedUserId, [...messages, adminMessage]);
                return newSessions;
            });

            setInputMessage('');
        }
    };

    const getMessageClassName = (type) => {
        switch (type) {
            case 'USER_MESSAGE':
                return 'admin-message-user';
            case 'ADMIN_MESSAGE':
                return 'admin-message-admin';
            case 'RULE_RESPONSE':
                return 'admin-message-system rule-based';
            case 'AI_RESPONSE':
                return 'admin-message-system ai-based';
            default:
                return 'admin-message-system';
        }
    };

    const selectedMessages = selectedUserId ? activeSessions.get(selectedUserId) || [] : [];
    const sessionCount = activeSessions.size;

    return (
        <div className="admin-chat-container">
            <div className="admin-chat-header">
                <h2>💬 Customer Support - Admin Panel</h2>
                <span className={`admin-chat-status ${isConnected ? 'connected' : 'disconnected'}`}>
                    {isConnected ? '🟢 Connected' : '🔴 Disconnected'}
                </span>
            </div>

            <div className="admin-chat-content">
                <div className="admin-chat-sessions">
                    <div className="admin-sessions-header">
                        <h3>Active Sessions ({sessionCount})</h3>
                    </div>
                    <div className="admin-sessions-list">
                        {Array.from(activeSessions.keys()).map(userId => {
                            const messages = activeSessions.get(userId);
                            const lastMessage = messages[messages.length - 1];
                            const unreadCount = messages.filter(m => m.type === 'USER_MESSAGE').length;

                            return (
                                <div
                                    key={userId}
                                    className={`admin-session-item ${selectedUserId === userId ? 'active' : ''}`}
                                    onClick={() => setSelectedUserId(userId)}
                                >
                                    <div className="session-user-icon">👤</div>
                                    <div className="session-info">
                                        <div className="session-user-name">
                                            User {userId}
                                            {unreadCount > 0 && (
                                                <span className="session-unread-badge">{unreadCount}</span>
                                            )}
                                        </div>
                                        <div className="session-last-message">
                                            {lastMessage?.content?.substring(0, 30)}...
                                        </div>
                                    </div>
                                    <div className="session-time">
                                        {new Date(lastMessage?.timestamp).toLocaleTimeString([], {
                                            hour: '2-digit',
                                            minute: '2-digit'
                                        })}
                                    </div>
                                </div>
                            );
                        })}
                        {sessionCount === 0 && (
                            <div className="admin-no-sessions">
                                <p>No active chat sessions</p>
                                <p className="admin-no-sessions-subtitle">
                                    Waiting for users to start conversations...
                                </p>
                            </div>
                        )}
                    </div>
                </div>

                <div className="admin-chat-messages-panel">
                    {selectedUserId ? (
                        <>
                            <div className="admin-messages-header">
                                <div className="admin-current-user">
                                    <div className="admin-user-avatar">👤</div>
                                    <div>
                                        <div className="admin-user-name">User {selectedUserId}</div>
                                        <div className="admin-user-status">Online</div>
                                    </div>
                                </div>
                            </div>

                            <div className="admin-messages-container">
                                {selectedMessages.map((msg, index) => (
                                    <div key={index} className={`admin-message ${getMessageClassName(msg.type)}`}>
                                        <div className="admin-message-sender">{msg.senderName}</div>
                                        <div className="admin-message-content">{msg.content}</div>
                                        <div className="admin-message-time">
                                            {new Date(msg.timestamp).toLocaleTimeString()}
                                        </div>
                                        {msg.type === 'RULE_RESPONSE' && (
                                            <div className="admin-message-badge">🤖 Rule-Based</div>
                                        )}
                                        {msg.type === 'AI_RESPONSE' && (
                                            <div className="admin-message-badge">🧠 AI-Powered</div>
                                        )}
                                    </div>
                                ))}
                                <div ref={messagesEndRef} />
                            </div>

                            <form className="admin-chat-input-form" onSubmit={handleSendMessage}>
                                <input
                                    type="text"
                                    value={inputMessage}
                                    onChange={(e) => setInputMessage(e.target.value)}
                                    placeholder="Type your response..."
                                    className="admin-chat-input"
                                    disabled={!isConnected}
                                />
                                <button
                                    type="submit"
                                    className="admin-chat-send-btn"
                                    disabled={!isConnected || !inputMessage.trim()}
                                >
                                    Send
                                </button>
                            </form>
                        </>
                    ) : (
                        <div className="admin-no-selection">
                            <div className="admin-no-selection-icon">💬</div>
                            <h3>Select a conversation</h3>
                            <p>Choose a user from the list to view and respond to their messages</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default AdminChatPanel;