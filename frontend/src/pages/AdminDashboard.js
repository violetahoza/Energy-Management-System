import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { userAPI, deviceAPI } from '../services/api';
import '../styles/App.css';
import Alert from '../components/common/Alert';
import LoadingSpinner from '../components/common/LoadingSpinner';

const AdminDashboard = () => {
    const navigate = useNavigate();
    const { user, logout } = useAuth();
    const [activeTab, setActiveTab] = useState('users');
    const [users, setUsers] = useState([]);
    const [devices, setDevices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [showUserModal, setShowUserModal] = useState(false);
    const [showDeviceModal, setShowDeviceModal] = useState(false);
    const [showAssignModal, setShowAssignModal] = useState(false);
    const [selectedUser, setSelectedUser] = useState(null);
    const [selectedDevice, setSelectedDevice] = useState(null);

    const fetchData = async () => {
        setLoading(true);
        setError('');
        try {
            const [usersData, devicesData] = await Promise.all([
                userAPI.getAllUsers(),
                deviceAPI.getAllDevices()
            ]);
            setUsers(usersData);
            setDevices(devicesData);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (user?.role !== 'ADMIN') {
            navigate('/login');
        } else {
            fetchData();
        }
    }, [user, navigate]);

    const handleLogout = async () => {
        await logout();
        navigate('/login');
    };

    const handleDeleteUser = async (userId) => {
        if (!window.confirm('Are you sure you want to delete this user? This action cannot be undone.')) {
            return;
        }
        try {
            await userAPI.deleteUser(userId);
            await fetchData();
        } catch (err) {
            setError(err.message);
        }
    };

    const handleDeleteDevice = async (deviceId) => {
        if (!window.confirm('Are you sure you want to delete this device?')) {
            return;
        }
        try {
            await deviceAPI.deleteDevice(deviceId);
            await fetchData();
        } catch (err) {
            setError(err.message);
        }
    };

    const handleAssignDevice = async (deviceId, userId) => {
        try {
            if (userId) {
                await deviceAPI.assignDeviceToUser(deviceId, userId);
            } else {
                await deviceAPI.unassignDevice(deviceId);
            }
            await fetchData();
            setShowAssignModal(false);
            setSelectedDevice(null);
        } catch (err) {
            setError(err.message);
        }
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
                        <h1 className="page-title">Admin Dashboard</h1>
                        <p className="page-description">Manage users, devices, and system configuration</p>
                    </div>

                    {error && <Alert type="error" message={error} onClose={() => setError('')} />}

                    <div className="card">
                        <div className="card-header">
                            <div className="tab-buttons">
                                <button
                                    className={`btn btn-sm ${activeTab === 'users' ? 'btn-primary' : 'btn-secondary'}`}
                                    onClick={() => setActiveTab('users')}
                                >
                                    👥 Users
                                </button>
                                <button
                                    className={`btn btn-sm ${activeTab === 'devices' ? 'btn-primary' : 'btn-secondary'}`}
                                    onClick={() => setActiveTab('devices')}
                                >
                                    📱 Devices
                                </button>
                            </div>
                            <button
                                className="btn btn-sm btn-success"
                                onClick={() => activeTab === 'users' ? setShowUserModal(true) : setShowDeviceModal(true)}
                            >
                                ➕ Add {activeTab === 'users' ? 'User' : 'Device'}
                            </button>
                        </div>

                        {loading ? (
                            <LoadingSpinner message={`Loading ${activeTab}...`} />
                        ) : activeTab === 'users' ? (
                            <UsersTable
                                users={users}
                                onEdit={(user) => {
                                    setSelectedUser(user);
                                    setShowUserModal(true);
                                }}
                                onDelete={handleDeleteUser}
                            />
                        ) : (
                            <DevicesTable
                                devices={devices}
                                users={users}
                                onEdit={(device) => {
                                    setSelectedDevice(device);
                                    setShowDeviceModal(true);
                                }}
                                onDelete={handleDeleteDevice}
                                onAssign={(device) => {
                                    setSelectedDevice(device);
                                    setShowAssignModal(true);
                                }}
                            />
                        )}
                    </div>
                </div>
            </div>

            {showUserModal && (
                <UserModal
                    user={selectedUser}
                    onClose={() => {
                        setShowUserModal(false);
                        setSelectedUser(null);
                    }}
                    onSuccess={() => {
                        fetchData();
                        setShowUserModal(false);
                        setSelectedUser(null);
                    }}
                />
            )}

            {showDeviceModal && (
                <DeviceModal
                    device={selectedDevice}
                    users={users}
                    onClose={() => {
                        setShowDeviceModal(false);
                        setSelectedDevice(null);
                    }}
                    onSuccess={() => {
                        fetchData();
                        setShowDeviceModal(false);
                        setSelectedDevice(null);
                    }}
                />
            )}

            {showAssignModal && selectedDevice && (
                <AssignDeviceModal
                    device={selectedDevice}
                    users={users}
                    onClose={() => {
                        setShowAssignModal(false);
                        setSelectedDevice(null);
                    }}
                    onAssign={handleAssignDevice}
                />
            )}
        </div>
    );
};

const UsersTable = ({ users, onEdit, onDelete }) => {
    if (users.length === 0) {
        return (
            <div className="empty-state">
                <p className="empty-state-text">No users found</p>
            </div>
        );
    }

    return (
        <table className="table">
            <thead>
            <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Username</th>
                <th>Email</th>
                <th>Role</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            {users.map(user => (
                <tr key={user.userId}>
                    <td>{user.userId}</td>
                    <td>{user.firstName} {user.lastName}</td>
                    <td>{user.username}</td>
                    <td>{user.email}</td>
                    <td>
                        <span className="user-badge">{user.role}</span>
                    </td>
                    <td>
                        <div className="action-buttons">
                            <button className="btn btn-sm btn-edit" onClick={() => onEdit(user)} title="Edit user">✏️</button>
                            <button className="btn btn-sm btn-delete" onClick={() => onDelete(user.userId)} title="Delete user">🗑️</button>
                        </div>
                    </td>
                </tr>
            ))}
            </tbody>
        </table>
    );
};

const DevicesTable = ({ devices, users, onEdit, onDelete, onAssign }) => {
    const getUserName = (userId) => {
        const user = users.find(u => u.userId === userId);
        return user ? `${user.firstName} ${user.lastName}` : 'Unassigned';
    };

    if (devices.length === 0) {
        return (
            <div className="empty-state">
                <p className="empty-state-text">No devices found</p>
            </div>
        );
    }

    return (
        <table className="table">
            <thead>
            <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Description</th>
                <th>Location</th>
                <th>Max Consumption (kW)</th>
                <th>Assigned To</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            {devices.map(device => (
                <tr key={device.deviceId}>
                    <td>{device.deviceId}</td>
                    <td>{device.name}</td>
                    <td>{device.description}</td>
                    <td>{device.location}</td>
                    <td>{device.maximumConsumption}</td>
                    <td>{getUserName(device.userId)}</td>
                    <td>
                        <div className="action-buttons">
                            <button className="btn btn-sm btn-success" onClick={() => onAssign(device)} title="Assign device">👤</button>
                            <button className="btn btn-sm btn-edit" onClick={() => onEdit(device)} title="Edit device">✏️</button>
                            <button className="btn btn-sm btn-delete" onClick={() => onDelete(device.deviceId)} title="Delete device">🗑️</button>
                        </div>
                    </td>
                </tr>
            ))}
            </tbody>
        </table>
    );
};

const UserModal = ({ user, onClose, onSuccess }) => {
    const [formData, setFormData] = useState({
        firstName: user?.firstName || '',
        lastName: user?.lastName || '',
        email: user?.email || '',
        address: user?.address || '',
        username: user?.username || '',
        password: '',
        role: user?.role || 'CLIENT'
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (!user) {
            if (formData.password.length < 6) {
                setError('Password must be at least 6 characters long');
                return;
            }
        } else {
            if (formData.password && formData.password.length < 6) {
                setError('Password must be at least 6 characters long');
                return;
            }
        }

        setLoading(true);

        try {
            if (user) {
                const updateData = { ...formData };
                if (!updateData.password) {
                    delete updateData.password;
                }
                await userAPI.updateUser(user.userId, updateData);
            } else {
                await userAPI.createUser(formData);
            }
            onSuccess();
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2 className="modal-title">{user ? 'Edit User' : 'Create New User'}</h2>
                    <button className="modal-close" onClick={onClose}>✕</button>
                </div>
                <div className="modal-body">
                    {error && <Alert type="error" message={error} />}

                    <form onSubmit={handleSubmit}>
                        <div className="form-group">
                            <label className="form-label">First Name *</label>
                            <input
                                type="text"
                                className="form-input"
                                value={formData.firstName}
                                onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                                required
                                minLength="2"
                                maxLength="50"
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label">Last Name *</label>
                            <input
                                type="text"
                                className="form-input"
                                value={formData.lastName}
                                onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                                required
                                minLength="2"
                                maxLength="50"
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label">Email *</label>
                            <input
                                type="email"
                                className="form-input"
                                value={formData.email}
                                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label">Address *</label>
                            <input
                                type="text"
                                className="form-input"
                                value={formData.address}
                                onChange={(e) => setFormData({ ...formData, address: e.target.value })}
                                required
                                maxLength="200"
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label">Username *</label>
                            <input
                                type="text"
                                className="form-input"
                                value={formData.username}
                                onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                                required={!user}
                                disabled={!!user}
                                minLength="3"
                                maxLength="50"
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label">Password {user ? '(leave blank to keep current)' : '*'}</label>
                            <input
                                type="password"
                                className="form-input"
                                placeholder={user ? '(optional)' : 'Password (min 6 chars)'}
                                value={formData.password}
                                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                                required={!user}
                                minLength="6"
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label">Role *</label>
                            <select
                                className="form-select"
                                value={formData.role}
                                onChange={(e) => setFormData({ ...formData, role: e.target.value })}
                                required
                            >
                                <option value="CLIENT">CLIENT</option>
                                <option value="ADMIN">ADMIN</option>
                            </select>
                        </div>
                        <div className="modal-footer">
                            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
                            <button type="submit" className="btn btn-primary" disabled={loading}>{loading ? 'Saving...' : (user ? 'Update' : 'Create')}</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

const DeviceModal = ({ device, users, onClose, onSuccess }) => {
    const [formData, setFormData] = useState({
        name: device?.name || '',
        description: device?.description || '',
        location: device?.location || '',
        maximumConsumption: device?.maximumConsumption || '',
        userId: device?.userId || ''
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const clientUsers = users.filter(user => user.role === 'CLIENT');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const deviceData = {
                ...formData,
                userId: formData.userId || null,
                maximumConsumption: parseFloat(formData.maximumConsumption)
            };

            if (device) {
                await deviceAPI.updateDevice(device.deviceId, deviceData);
            } else {
                await deviceAPI.createDevice(deviceData);
            }
            onSuccess();
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2 className="modal-title">{device ? 'Edit Device' : 'Create New Device'}</h2>
                    <button className="modal-close" onClick={onClose}>✕</button>
                </div>
                <div className="modal-body">
                    {error && <Alert type="error" message={error} />}

                    <form onSubmit={handleSubmit}>
                        <div className="form-group">
                            <label className="form-label">Device Name *</label>
                            <input
                                type="text"
                                className="form-input"
                                value={formData.name}
                                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label">Description *</label>
                            <input
                                type="text"
                                className="form-input"
                                value={formData.description}
                                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label">Location *</label>
                            <input
                                type="text"
                                className="form-input"
                                value={formData.location}
                                onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label">Maximum Consumption (kW) *</label>
                            <input
                                type="number"
                                step="0.01"
                                className="form-input"
                                value={formData.maximumConsumption}
                                onChange={(e) => setFormData({ ...formData, maximumConsumption: e.target.value })}
                                required
                                min="0"
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label">Assign to User (Optional)</label>
                            <select className="form-select" value={formData.userId} onChange={(e) => setFormData({ ...formData, userId: e.target.value })}>
                                <option value="">Unassigned</option>
                                {clientUsers.map(user => (
                                    <option key={user.userId} value={user.userId}>
                                        {user.firstName} {user.lastName} ({user.username})
                                    </option>
                                ))}
                            </select>
                        </div>
                        <div className="modal-footer">
                            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
                            <button type="submit" className="btn btn-primary" disabled={loading}>{loading ? 'Saving...' : (device ? 'Update' : 'Create')}</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

const AssignDeviceModal = ({ device, users, onClose, onAssign }) => {
    const [selectedUserId, setSelectedUserId] = useState(device.userId || '');

    const clientUsers = users.filter(user => user.role === 'CLIENT');

    const handleSubmit = (e) => {
        e.preventDefault();
        onAssign(device.deviceId, selectedUserId || null);
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal modal-sm" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2 className="modal-title">Assign Device</h2>
                    <button className="modal-close" onClick={onClose}>✕</button>
                </div>
                <div className="modal-body">
                    <form onSubmit={handleSubmit}>
                        <div className="form-group">
                            <label className="form-label">Device: {device.name}</label>
                            <p className="form-label">Select a client to assign this device to, or leave unassigned.</p>
                            {clientUsers.length === 0 && (
                                <div className="alert alert-warning">
                                    No client users available for assignment. Only users with CLIENT role can be assigned devices.
                                </div>
                            )}
                        </div>
                        <div className="form-group">
                            <label className="form-label">Assign to User</label>
                            <select className="form-select" value={selectedUserId} onChange={(e) => setSelectedUserId(e.target.value)} disabled={clientUsers.length === 0}>
                                <option value="">Unassigned</option>
                                {clientUsers.map(user => (
                                    <option key={user.userId} value={user.userId}>{user.firstName} {user.lastName} ({user.username})</option>
                                ))}
                            </select>
                        </div>
                        <div className="modal-footer">
                            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
                            <button type="submit" className="btn btn-primary">Assign</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default AdminDashboard;