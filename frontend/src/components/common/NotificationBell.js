import React, { useState, useEffect } from 'react';
import websocketService from '../../services/websocket';
import { useAuth } from '../../context/AuthContext';
import '../../styles/App.css';

const NotificationBell = () => {
    const { user } = useAuth();
    const [alerts, setAlerts] = useState([]);
    const [showDropdown, setShowDropdown] = useState(false);
    const [unreadCount, setUnreadCount] = useState(0);

    useEffect(() => {
        if (user) {
            const token = localStorage.getItem('token');

            websocketService.connect(user.userId, token, {
                onAlert: (alert) => {
                    setAlerts(prev => [alert, ...prev]);
                    setUnreadCount(prev => prev + 1);
                }
            });
        }
    }, [user]);

    const markAsRead = (index) => {
        setAlerts(prev => {
            const newAlerts = [...prev];
            if (!newAlerts[index].read) {
                newAlerts[index].read = true;
                setUnreadCount(count => Math.max(0, count - 1));
            }
            return newAlerts;
        });
    };

    const markAllAsRead = () => {
        setAlerts(prev => prev.map(alert => ({ ...alert, read: true })));
        setUnreadCount(0);
    };

    const clearAll = () => {
        setAlerts([]);
        setUnreadCount(0);
    };

    return (
        <div className="notification-container">
            <button
                className="notification-bell"
                onClick={() => setShowDropdown(!showDropdown)}
            >
                🔔
                {unreadCount > 0 && (
                    <span className="notification-badge">{unreadCount}</span>
                )}
            </button>

            {showDropdown && (
                <div className="notification-dropdown">
                    <div className="notification-header">
                        <h3>Notifications</h3>
                        <div className="notification-actions">
                            {alerts.length > 0 && (
                                <>
                                    <button onClick={markAllAsRead}>Mark all read</button>
                                    <button onClick={clearAll}>Clear all</button>
                                </>
                            )}
                        </div>
                    </div>

                    <div className="notification-list">
                        {alerts.length === 0 ? (
                            <div className="no-notifications">
                                No notifications
                            </div>
                        ) : (
                            alerts.map((alert, index) => (
                                <div
                                    key={index}
                                    className={`notification-item ${alert.read ? 'read' : 'unread'}`}
                                    onClick={() => markAsRead(index)}
                                >
                                    <div className="notification-icon">⚠️</div>
                                    <div className="notification-content">
                                        <div className="notification-message">
                                            {alert.message}
                                        </div>
                                        <div className="notification-details">
                                            Device #{alert.deviceId} exceeded limit by{' '}
                                            {alert.exceededBy.toFixed(2)} kWh
                                        </div>
                                        <div className="notification-time">
                                            {new Date(alert.timestamp).toLocaleString()}
                                        </div>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default NotificationBell;