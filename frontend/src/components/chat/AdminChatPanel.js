import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useWebSocket } from '../../context/WebSocketContext';
import '../../styles/AdminChat.css';

const AdminChatPanel = ({ chatSessions, setChatSessions }) => {
    const { user } = useAuth();
    const { isConnected, connect, sendAdminResponse } = useWebSocket();

    const [selectedUserId, setSelectedUserId] = useState(null);
    const [inputMessage, setInputMessage] = useState('');
    const messagesEndRef = useRef(null);

    useEffect(() => {
        scrollToBottom();
    }, [selectedUserId, chatSessions]);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    const handleSendMessage = (e) => {
        e.preventDefault();
        if (inputMessage.trim() && selectedUserId && isConnected) {
            sendAdminResponse(selectedUserId, inputMessage);
            setInputMessage('');
        }
    };

    const handleReconnect = () => {
        const token = localStorage.getItem('token');
        connect(user.userId, token, true);
    };

    const getMessageClassName = (type) => {
        switch (type) {
            case 'USER_MESSAGE': return 'admin-message-user';
            case 'ADMIN_MESSAGE': return 'admin-message-admin';
            case 'RULE_RESPONSE': return 'admin-message-system rule-based';
            case 'AI_RESPONSE': return 'admin-message-system ai-based';
            default: return 'admin-message-system';
        }
    };

    const clearSession = (userId) => {
        if (window.confirm(`Clear chat history with User ${userId}?`)) {
            setChatSessions(prev => {
                const newSessions = new Map(prev);
                newSessions.delete(userId);
                return newSessions;
            });
            if (selectedUserId === userId) {
                setSelectedUserId(null);
            }
        }
    };

    const selectedMessages = selectedUserId ? chatSessions.get(selectedUserId) || [] : [];
    const sessionCount = chatSessions.size;

    return (
        <div className="admin-chat-container">
            <div className="admin-chat-header">
                <h2>💬 Customer Support</h2>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <span className={`admin-chat-status ${isConnected ? 'connected' : 'disconnected'}`}>
                        {isConnected ? '🟢 Connected' : '🔴 Disconnected'}
                    </span>
                    {!isConnected && (<button className="btn btn-sm btn-primary" onClick={handleReconnect}>Reconnect</button>)}
                </div>
            </div>

            <div className="admin-chat-content">
                <div className="admin-chat-sessions">
                    <div className="admin-sessions-header">
                        <h3>Active Sessions ({sessionCount})</h3>
                    </div>
                    <div className="admin-sessions-list">
                        {Array.from(chatSessions.keys()).map(userId => {
                            const messages = chatSessions.get(userId);
                            const lastMessage = messages[messages.length - 1];

                            return (
                                <div
                                    key={userId}
                                    className={`admin-session-item ${selectedUserId === userId ? 'active' : ''}`}
                                    onClick={() => setSelectedUserId(userId)}
                                >
                                    <div className="session-user-icon">👤</div>
                                    <div className="session-info">
                                        <div className="session-user-name">User {userId}</div>
                                        <div className="session-last-message">
                                            {lastMessage?.content?.substring(0, 30)}
                                            {lastMessage?.content?.length > 30 ? '...' : ''}
                                        </div>
                                    </div>
                                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '5px' }}>
                                        <div className="session-time">
                                            {lastMessage && new Date(lastMessage.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                        </div>
                                        <button
                                            className="btn btn-sm btn-delete"
                                            onClick={(e) => { e.stopPropagation(); clearSession(userId); }}
                                            title="Clear chat"
                                            style={{ fontSize: '12px', padding: '2px 6px' }}
                                        >
                                            🗑️
                                        </button>
                                    </div>
                                </div>
                            );
                        })}
                        {sessionCount === 0 && (
                            <div className="admin-no-sessions">
                                <p>📭 No active chat sessions</p>
                                <p className="admin-no-sessions-subtitle">Waiting for users to start conversations...</p>
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
                                        <div className="admin-user-status">{isConnected ? '🟢 Online' : '🔴 Offline'}</div>
                                    </div>
                                </div>
                                <button className="btn btn-sm btn-secondary" onClick={() => clearSession(selectedUserId)} title="Clear this conversation">🗑️ Clear</button>
                            </div>

                            <div className="admin-messages-container">
                                {selectedMessages.length === 0 && (<div className="admin-no-messages"><p>No messages yet</p></div>)}
                                {selectedMessages.map((msg, index) => (
                                    <div key={`${msg.timestamp}-${index}`} className={`admin-message ${getMessageClassName(msg.type)}`}>
                                        <div className="admin-message-sender">{msg.senderName}</div>
                                        <div className="admin-message-content">{msg.content}</div>
                                        <div className="admin-message-time">{new Date(msg.timestamp).toLocaleTimeString()}</div>
                                        {msg.type === 'RULE_RESPONSE' && <div className="admin-message-badge">🤖 Rule-Based</div>}
                                        {msg.type === 'AI_RESPONSE' && <div className="admin-message-badge">🧠 AI-Powered</div>}
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
                                <button type="submit" className="admin-chat-send-btn" disabled={!isConnected || !inputMessage.trim()}>Send</button>
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