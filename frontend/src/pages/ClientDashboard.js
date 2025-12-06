import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { deviceAPI } from '../services/api';
import Alert from '../components/common/Alert';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ChatWidget from "../components/chat/ChatWidget";
import NotificationBell from "../components/common/NotificationBell";
import EnergyConsumptionChart from '../components/charts/EnergyConsumptionChart';
import TotalUserConsumptionChart from '../components/charts/TotalUserConsumptionChart';
import '../styles/App.css';

const ClientDashboard = () => {
    const navigate = useNavigate();
    const { user, logout } = useAuth();
    const [devices, setDevices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [selectedDevice, setSelectedDevice] = useState(null);
    const [showConsumptionModal, setShowConsumptionModal] = useState(false);
    const [showTotalConsumptionModal, setShowTotalConsumptionModal] = useState(false);
    const [selectedDeviceForChart, setSelectedDeviceForChart] = useState(null);

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

    const handleViewConsumption = (device) => {
        setSelectedDeviceForChart(device);
        setShowConsumptionModal(true);
    };

    const handleViewTotalConsumption = () => {
        setShowTotalConsumptionModal(true);
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
                        <NotificationBell />
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

                    {error && <Alert type="error" message={error} onClose={() => setError('')}/>}

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

                        {devices.length > 0 && (
                            <div className="card card-center" style={{ cursor: 'pointer' }} onClick={handleViewTotalConsumption}>
                                <div className="stat-card-icon">📊</div>
                                <div className="stat-card-value">View</div>
                                <div className="stat-card-label">Total Consumption Chart</div>
                            </div>
                        )}
                    </div>

                    <div className="card">
                        <div className="card-header"><h2 className="card-title">Devices</h2></div>

                        {loading ? (
                            <LoadingSpinner message="Loading your devices..."/>
                        ) : devices.length === 0 ? (
                            <div className="empty-state">
                                <div className="empty-state-icon">📱</div>
                                <p className="empty-state-text">No devices assigned to you yet</p>
                                <p className="empty-state-subtext">Contact your administrator to get devices assigned</p>
                            </div>
                        ) : (
                            <div className="device-grid">
                                {devices.map(device => (
                                    <DeviceCard
                                        key={device.deviceId}
                                        device={device}
                                        onViewDetails={() => setSelectedDevice(device)}
                                        onViewConsumption={() => handleViewConsumption(device)}
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
                    onViewConsumption={() => {
                        setSelectedDevice(null);
                        handleViewConsumption(selectedDevice);
                    }}
                />
            )}

            {showConsumptionModal && selectedDeviceForChart && (
                <ConsumptionModal
                    device={selectedDeviceForChart}
                    onClose={() => {setShowConsumptionModal(false);setSelectedDeviceForChart(null);}}
                />
            )}

            {showTotalConsumptionModal && (<TotalConsumptionModal devices={devices} onClose={() => setShowTotalConsumptionModal(false)}/>)}

            <ChatWidget />
        </div>
    );
};

const DeviceCard = ({device, onViewDetails, onViewConsumption}) => {
    return (
        <div className="card device-card">
            <div className="device-card-header">
                <div className="device-card-icon">📱</div>
                <div className="device-card-info">
                    <h3 className="device-card-name">{device.name}</h3>
                    <p className="device-card-location">{device.location}</p>
                </div>
            </div>

            <div className="device-card-consumption">
                <div className="device-card-consumption-value">{device.maximumConsumption} kWh</div>
                <div className="device-card-consumption-label">Maximum Consumption</div>
            </div>

            <p className="device-card-description">{device.description}</p>

            <div className="device-card-actions">
                <button className="btn btn-sm btn-secondary" onClick={onViewDetails}>📋 Details</button>
                <button className="btn btn-sm btn-primary" onClick={onViewConsumption}>📊 View Consumption</button>
            </div>
        </div>
    );
};

const DeviceDetailModal = ({ device, onClose, onViewConsumption }) => {
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
                        <DetailRow label="Maximum Consumption" value={`${device.maximumConsumption} kWh`} highlight/>
                        <DetailRow label="Created At" value={new Date(device.createdAt).toLocaleString()}/>
                        <DetailRow label="Last Updated" value={new Date(device.updatedAt).toLocaleString()}/>
                    </div>
                </div>
                <div className="modal-footer">
                    <button className="btn btn-secondary" onClick={onClose}>Close</button>
                    <button className="btn btn-primary" onClick={onViewConsumption}>📊 View Energy Consumption</button>
                </div>
            </div>
        </div>
    );
};

const ConsumptionModal = ({ device, onClose }) => {
    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal modal-xl" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <div className="modal-header-content">
                        <div className="modal-header-icon">📊</div>
                        <h2 className="modal-title">Energy Consumption - {device.name}</h2>
                    </div>
                    <button className="modal-close" onClick={onClose}>✕</button>
                </div>
                <div className="modal-body"><EnergyConsumptionChart device={device} /></div>
                <div className="modal-footer">
                    <button className="btn btn-primary" onClick={onClose}>Close</button>
                </div>
            </div>
        </div>
    );
};

const TotalConsumptionModal = ({ devices, onClose }) => {
    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal modal-xl" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <div className="modal-header-content">
                        <div className="modal-header-icon">📊</div>
                        <h2 className="modal-title">Total Energy Consumption - All Devices</h2>
                    </div>
                    <button className="modal-close" onClick={onClose}>✕</button>
                </div>
                <div className="modal-body"><TotalUserConsumptionChart devices={devices} /></div>
                <div className="modal-footer"><button className="btn btn-primary" onClick={onClose}>Close</button></div>
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