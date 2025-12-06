import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../../context/AuthContext';
import '../../styles/App.css';
import { useWebSocket } from '../../context/WebSocketContext';

const NotificationBell = () => {
    const websocketService = useWebSocket();
    const { user } = useAuth();
    const [alerts, setAlerts] = useState([]);
    const [showDropdown, setShowDropdown] = useState(false);
    const [unreadCount, setUnreadCount] = useState(0);
    const dropdownRef = useRef(null);

    useEffect(() => {
        if (!websocketService) return;

        if (user) {
            const token = localStorage.getItem('token');

            const handleAlert = (alert) => {
                console.log('NotificationBell received alert:', alert);
                setAlerts(prev => [alert, ...prev]);
                setUnreadCount(prev => prev + 1);
            };

            websocketService.subscribe('alerts', handleAlert);

            return () => {
                websocketService.unsubscribe('alerts', handleAlert);
            };
        }
    }, [user]);

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setShowDropdown(false);
            }
        };

        if (showDropdown) {
            document.addEventListener('mousedown', handleClickOutside);
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [showDropdown]);

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
        <div className="notification-container" ref={dropdownRef}>
            <button
                className="notification-bell"
                onClick={() => setShowDropdown(!showDropdown)}
                aria-label="Notifications"
            >
                🔔
                {unreadCount > 0 && (<span className="notification-badge">{unreadCount}</span>)}
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
                                <div className="no-notifications-icon">🔕</div>
                                <p>No notifications yet</p>
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
                                        <div className="notification-title">
                                            Overconsumption Alert
                                        </div>
                                        <div className="notification-message">
                                            Device #{alert.deviceId} exceeded limit by{' '}
                                            {alert.exceededBy?.toFixed(2)} kWh
                                        </div>
                                        <div className="notification-details">
                                            Current: {alert.currentConsumption?.toFixed(2)} kWh |
                                            Max: {alert.maxConsumption?.toFixed(2)} kWh
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