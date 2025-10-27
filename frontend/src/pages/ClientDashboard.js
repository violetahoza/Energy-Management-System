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
                    <div className="navbar-left">
                        <div className="navbar-brand">
                            <span className="navbar-brand-icon">⚡</span>
                            <span>Energy Management System</span>
                        </div>
                        <div className="navbar-divider"></div>
                        <span className="navbar-welcome">Welcome {user?.username}!</span>
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
                        <p className="page-description">View and monitor your assigned energy devices</p>
                    </div>

                    {error && (
                        <div className="alert alert-error">
                            <div className="alert-content">
                                <span className="alert-icon">⚠</span>
                                <span>{error}</span>
                            </div>
                            <button onClick={() => setError('')} className="alert-close" aria-label="Close alert"> ✕ </button>
                        </div>
                    )}

                    <div className="stats-grid">
                        <div className="card card-center">
                            <div className="stat-card-icon">📱</div>
                            <div className="stat-card-value">{devices.length}</div>
                            <div className="stat-card-label">Total Devices</div>
                        </div>

                        <div className="card card-center">
                            <div className="stat-card-icon">⚡</div>
                            <div className="stat-card-value">{getTotalConsumption()}</div>
                            <div className="stat-card-label">Total Max Consumption (kWh)</div>
                        </div>
                    </div>

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
                                <div className="empty-state-icon">📱</div>
                                <p className="empty-state-text">No devices assigned to you yet</p>
                                <p className="empty-state-subtext">Contact your administrator to get devices assigned</p>
                            </div>
                        ) : (
                            <div className="device-grid">
                                {devices.map(device => (
                                    <DeviceCard key={device.deviceId} device={device} onClick={() => setSelectedDevice(device)}/>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {selectedDevice && (
                <DeviceDetailModal device={selectedDevice} onClose={() => setSelectedDevice(null)}/>
            )}
        </div>
    );
};

const DeviceCard = ({ device, onClick }) => {
    return (
        <div className="card device-card" onClick={onClick}>
            <div className="device-card-header">
                <div className="device-card-icon">📱</div>
                <div className="device-card-info">
                    <h3 className="device-card-name">{device.name}</h3>
                    <p className="device-card-location">{device.location}</p>
                </div>
            </div>

            <div className="device-card-consumption">
                <div className="device-card-consumption-value">
                    {device.maximumConsumption} kW
                </div>
                <div className="device-card-consumption-label">
                    Maximum Consumption
                </div>
            </div>

            <p className="device-card-description">{device.description}</p>

            <div className="device-card-footer">
                <span>ID: {device.deviceId}</span>
                <span>Click for details →</span>
            </div>
        </div>
    );
};

const DeviceDetailModal = ({ device, onClose }) => {
    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal modal-wide" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <div className="modal-header-content">
                        <div className="modal-header-icon">📱</div>
                        <h2 className="modal-title">{device.name}</h2>
                    </div>
                    <button className="modal-close" onClick={onClose}>✕</button>
                </div>
                <div className="modal-body">
                    <div className="detail-rows">
                        <DetailRow label="Device ID" value={device.deviceId} />
                        <DetailRow label="Name" value={device.name} />
                        <DetailRow label="Description" value={device.description} />
                        <DetailRow label="Location" value={device.location} />
                        <DetailRow label="Maximum Consumption" value={`${device.maximumConsumption} kW`} highlight/>
                        <DetailRow label="Created At" value={new Date(device.createdAt).toLocaleString()}/>
                        <DetailRow label="Last Updated" value={new Date(device.updatedAt).toLocaleString()}/>
                    </div>
                </div>
                <div className="modal-footer">
                    <button className="btn btn-primary" onClick={onClose}> Close </button>
                </div>
            </div>
        </div>
    );
};

const DetailRow = ({ label, value, highlight }) => {
    return (
        <div className={`detail-row ${highlight ? 'detail-row-highlight' : ''}`}>
            <span className="detail-row-label">{label}</span>
            <span className={`detail-row-value ${highlight ? 'detail-row-value-highlight' : ''}`}>{value}</span>
        </div>
    );
};

export default ClientDashboard;