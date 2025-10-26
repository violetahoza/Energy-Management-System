const API_URL = 'http://localhost';

const getAuthHeader = () => {
    const token = localStorage.getItem('token');
    return token ? { 'Authorization': `Bearer ${token}` } : {};
};

const handleResponse = async (response) => {
    if (!response.ok) {
        const error = await response.json().catch(() => ({
            message: 'An error occurred'
        }));
        throw new Error(error.message || `HTTP error! status: ${response.status}`);
    }
    return response.json();
};

export const userAPI = {
    getAllUsers: async () => {
        const response = await fetch(`${API_URL}/api/users`, {
            headers: {
                ...getAuthHeader(),
                'Content-Type': 'application/json'
            }
        });
        return handleResponse(response);
    },

    getUserById: async (userId) => {
        const response = await fetch(`${API_URL}/api/users/${userId}`, {
            headers: {
                ...getAuthHeader(),
                'Content-Type': 'application/json'
            }
        });
        return handleResponse(response);
    },

    createUser: async (userData) => {
        const response = await fetch(`${API_URL}/api/users`, {
            method: 'POST',
            headers: {
                ...getAuthHeader(),
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(userData)
        });
        return handleResponse(response);
    },

    updateUser: async (userId, userData) => {
        const response = await fetch(`${API_URL}/api/users/${userId}`, {
            method: 'PATCH',
            headers: {
                ...getAuthHeader(),
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(userData)
        });
        return handleResponse(response);
    },

    deleteUser: async (userId) => {
        const response = await fetch(`${API_URL}/api/users/${userId}`, {
            method: 'DELETE',
            headers: getAuthHeader()
        });
        if (!response.ok) {
            throw new Error('Failed to delete user');
        }
        return true;
    }
};

export const deviceAPI = {
    getAllDevices: async () => {
        const response = await fetch(`${API_URL}/api/devices`, {
            headers: {
                ...getAuthHeader(),
                'Content-Type': 'application/json'
            }
        });
        return handleResponse(response);
    },

    getDeviceById: async (deviceId) => {
        const response = await fetch(`${API_URL}/api/devices/${deviceId}`, {
            headers: {
                ...getAuthHeader(),
                'Content-Type': 'application/json'
            }
        });
        return handleResponse(response);
    },

    getDevicesByUserId: async (userId) => {
        const response = await fetch(`${API_URL}/api/devices/user/${userId}`, {
            headers: {
                ...getAuthHeader(),
                'Content-Type': 'application/json'
            }
        });
        return handleResponse(response);
    },

    createDevice: async (deviceData) => {
        const response = await fetch(`${API_URL}/api/devices`, {
            method: 'POST',
            headers: {
                ...getAuthHeader(),
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(deviceData)
        });
        return handleResponse(response);
    },

    updateDevice: async (deviceId, deviceData) => {
        const response = await fetch(`${API_URL}/api/devices/${deviceId}`, {
            method: 'PATCH',
            headers: {
                ...getAuthHeader(),
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(deviceData)
        });
        return handleResponse(response);
    },

    deleteDevice: async (deviceId) => {
        const response = await fetch(`${API_URL}/api/devices/${deviceId}`, {
            method: 'DELETE',
            headers: getAuthHeader()
        });
        if (!response.ok) {
            throw new Error('Failed to delete device');
        }
        return true;
    },

    assignDeviceToUser: async (deviceId, userId) => {
        const response = await fetch(`${API_URL}/api/devices/${deviceId}/assign/${userId}`, {
            method: 'PATCH',
            headers: {
                ...getAuthHeader(),
                'Content-Type': 'application/json'
            }
        });
        return handleResponse(response);
    },

    unassignDevice: async (deviceId) => {
        const response = await fetch(`${API_URL}/api/devices/${deviceId}/unassign`, {
            method: 'PATCH',
            headers: {
                ...getAuthHeader(),
                'Content-Type': 'application/json'
            }
        });
        return handleResponse(response);
    }
};

export default { userAPI, deviceAPI };