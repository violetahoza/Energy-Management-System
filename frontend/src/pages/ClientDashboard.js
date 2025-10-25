import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { deviceAPI } from '../services/api';
import '../styles/App.css';

const ClientDashboard = () => {
    const navigate = useNavigate();
    const { user, logout } = useAuth();
    const [devices, setDevices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [selectedDevice, setSelectedDevice] = useState(null);

    useEffect(() => {
        if (user?.role !== 'CLIENT') {
            navigate('/login');
        } else {
            fetchDevices();
        }
    }, [user, navigate]);

    const fetchDevices = async () => {
        setLoading(true);
        setError('');
        try {
            const devicesData = await deviceAPI.getDevicesByUserId(user.userId);
            setDevices(devicesData);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleLogout = async () => {
        await logout();
        navigate('/login');
    };

    const getTotalConsumption = () => {
        return devices.reduce((sum, device) => sum + (device.maximumConsumption || 0), 0).toFixed(2);
    };

    return (
        <div className="dashboard-container">
            <div className="dashboard-content">
                <nav className="navbar">
                    <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                        <div className="navbar-brand">
                            <span style={{ fontSize: '24px' }}>⚡</span>
                            <span>Energy Management System</span>
                        </div>
                        <div style={{
                            height: '24px',
                            width: '1px',
                            background: 'rgba(255, 255, 255, 0.2)'
                        }}></div>
                        <span style={{
                            color: '#00b4ff',
                            fontSize: '16px',
                            fontWeight: '500'
                        }}>
                            Welcome {user?.username}!
                        </span>
                    </div>
                    <div className="navbar-user">
                        <span className="user-badge">{user?.role}</span>
                        <button onClick={handleLogout} className="btn-logout">
                            <span>🚪</span>
                            <span>Logout</span>
                        </button>
                    </div>
                </nav>

                <div className="main-content">
                    <div className="page-header">
                        <h1 className="page-title">My Devices</h1>
                        <p className="page-description">
                            View and monitor your assigned energy devices
                        </p>
                    </div>

                    {error && (
                        <div className="alert alert-error" style={{ marginBottom: '24px', position: 'relative' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flex: 1 }}>
                                <span className="alert-icon">⚠</span>
                                <span>{error}</span>
                            </div>
                            <button
                                onClick={() => setError('')}
                                style={{
                                    position: 'absolute',
                                    right: '12px',
                                    top: '50%',
                                    transform: 'translateY(-50%)',
                                    background: 'none',
                                    border: 'none',
                                    color: 'inherit',
                                    cursor: 'pointer',
                                    fontSize: '20px',
                                    padding: '4px 8px',
                                    opacity: '0.7',
                                    transition: 'opacity 0.2s'
                                }}
                                onMouseEnter={(e) => e.target.style.opacity = '1'}
                                onMouseLeave={(e) => e.target.style.opacity = '0.7'}
                                aria-label="Close alert"
                            >
                                ✕
                            </button>
                        </div>
                    )}

                    {/* Statistics Cards */}
                    <div style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
                        gap: '20px',
                        marginBottom: '32px'
                    }}>
                        <div className="card" style={{ textAlign: 'center', padding: '32px' }}>
                            <div style={{ fontSize: '48px', marginBottom: '12px' }}>📱</div>
                            <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#00b4ff', marginBottom: '8px' }}>
                                {devices.length}
                            </div>
                            <div style={{ color: 'rgba(255,255,255,0.6)', fontSize: '14px' }}>
                                Total Devices
                            </div>
                        </div>

                        <div className="card" style={{ textAlign: 'center', padding: '32px' }}>
                            <div style={{ fontSize: '48px', marginBottom: '12px' }}>⚡</div>
                            <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#00b4ff', marginBottom: '8px' }}>
                                {getTotalConsumption()}
                            </div>
                            <div style={{ color: 'rgba(255,255,255,0.6)', fontSize: '14px' }}>
                                Total Max Consumption (kWh)
                            </div>
                        </div>
                    </div>

                    {/* Devices List */}
                    <div className="card">
                        <div className="card-header">
                            <h2 className="card-title">Devices</h2>
                        </div>

                        {loading ? (
                            <div className="empty-state">
                                <p className="empty-state-text">Loading your devices...</p>
                            </div>
                        ) : devices.length === 0 ? (
                            <div className="empty-state">
                                <div style={{ fontSize: '64px', marginBottom: '16px', opacity: 0.3 }}>
                                    📱
                                </div>
                                <p className="empty-state-text">
                                    No devices assigned to you yet
                                </p>
                                <p style={{
                                    color: 'rgba(255,255,255,0.4)',
                                    fontSize: '14px',
                                    marginTop: '8px'
                                }}>
                                    Contact your administrator to get devices assigned
                                </p>
                            </div>
                        ) : (
                            <div style={{
                                display: 'grid',
                                gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
                                gap: '20px',
                                padding: '20px'
                            }}>
                                {devices.map(device => (
                                    <DeviceCard
                                        key={device.deviceId}
                                        device={device}
                                        onClick={() => setSelectedDevice(device)}
                                    />
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {selectedDevice && (
                <DeviceDetailModal
                    device={selectedDevice}
                    onClose={() => setSelectedDevice(null)}
                />
            )}
        </div>
    );
};

// Device Card Component
const DeviceCard = ({ device, onClick }) => {
    return (
        <div
            className="card"
            style={{
                cursor: 'pointer',
                transition: 'all 0.3s ease',
                padding: '20px'
            }}
            onClick={onClick}
            onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-4px)';
                e.currentTarget.style.borderColor = 'rgba(0, 180, 255, 0.5)';
            }}
            onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.borderColor = 'rgba(0, 180, 255, 0.2)';
            }}
        >
            <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                marginBottom: '16px'
            }}>
                <div style={{
                    width: '48px',
                    height: '48px',
                    background: 'linear-gradient(135deg, #00b4ff, #0066ff)',
                    borderRadius: '12px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '24px'
                }}>
                    📱
                </div>
                <div style={{ flex: 1 }}>
                    <h3 style={{
                        margin: 0,
                        color: '#ffffff',
                        fontSize: '18px',
                        fontWeight: '600'
                    }}>
                        {device.name}
                    </h3>
                    <p style={{
                        margin: '4px 0 0 0',
                        color: 'rgba(255,255,255,0.6)',
                        fontSize: '13px'
                    }}>
                        {device.location}
                    </p>
                </div>
            </div>

            <div style={{
                padding: '12px',
                background: 'rgba(0, 180, 255, 0.1)',
                borderRadius: '8px',
                marginBottom: '12px'
            }}>
                <div style={{
                    fontSize: '24px',
                    fontWeight: 'bold',
                    color: '#00b4ff',
                    marginBottom: '4px'
                }}>
                    {device.maximumConsumption} kWh
                </div>
                <div style={{
                    fontSize: '12px',
                    color: 'rgba(255,255,255,0.6)',
                    textTransform: 'uppercase',
                    letterSpacing: '0.5px'
                }}>
                    Maximum Consumption
                </div>
            </div>

            <p style={{
                margin: 0,
                color: 'rgba(255,255,255,0.7)',
                fontSize: '14px',
                lineHeight: '1.5'
            }}>
                {device.description}
            </p>

            <div style={{
                marginTop: '16px',
                paddingTop: '16px',
                borderTop: '1px solid rgba(255,255,255,0.1)',
                display: 'flex',
                justifyContent: 'space-between',
                fontSize: '12px',
                color: 'rgba(255,255,255,0.5)'
            }}>
                <span>ID: {device.deviceId}</span>
                <span>Click for details →</span>
            </div>
        </div>
    );
};

// Device Detail Modal Component
const DeviceDetailModal = ({ device, onClose }) => {
    return (
        <div className="modal-overlay" onClick={onClose}>
            <div
                className="modal"
                style={{ maxWidth: '600px' }}
                onClick={(e) => e.stopPropagation()}
            >
                <div className="modal-header">
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <div style={{
                            width: '40px',
                            height: '40px',
                            background: 'linear-gradient(135deg, #00b4ff, #0066ff)',
                            borderRadius: '10px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontSize: '20px'
                        }}>
                            📱
                        </div>
                        <h2 className="modal-title">{device.name}</h2>
                    </div>
                    <button className="modal-close" onClick={onClose}>✕</button>
                </div>
                <div className="modal-body">
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                        <DetailRow label="Device ID" value={device.deviceId} />
                        <DetailRow label="Name" value={device.name} />
                        <DetailRow label="Description" value={device.description} />
                        <DetailRow label="Location" value={device.location} />
                        <DetailRow
                            label="Maximum Consumption"
                            value={`${device.maximumConsumption} kWh`}
                            highlight
                        />
                        <DetailRow
                            label="Created At"
                            value={new Date(device.createdAt).toLocaleString()}
                        />
                        <DetailRow
                            label="Last Updated"
                            value={new Date(device.updatedAt).toLocaleString()}
                        />
                    </div>
                </div>
                <div className="modal-footer">
                    <button className="btn btn-primary" onClick={onClose}>
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
};

// Detail Row Component
const DetailRow = ({ label, value, highlight }) => {
    return (
        <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '12px',
            background: highlight ? 'rgba(0, 180, 255, 0.1)' : 'rgba(255, 255, 255, 0.03)',
            borderRadius: '8px',
            border: highlight ? '1px solid rgba(0, 180, 255, 0.3)' : 'none'
        }}>
            <span style={{
                color: 'rgba(255,255,255,0.6)',
                fontSize: '14px',
                fontWeight: '500'
            }}>
                {label}
            </span>
            <span style={{
                color: highlight ? '#00b4ff' : '#ffffff',
                fontSize: highlight ? '18px' : '14px',
                fontWeight: highlight ? '600' : '500',
                textAlign: 'right'
            }}>
                {value}
            </span>
        </div>
    );
};

export default ClientDashboard;