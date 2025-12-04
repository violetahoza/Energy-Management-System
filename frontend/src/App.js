import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { WebSocketProvider } from './context/WebSocketContext';
import Login from './components/auth/Login';
// import Register from './components/auth/Register';
import AdminDashboard from './pages/AdminDashboard';
import ClientDashboard from './pages/ClientDashboard';
import './styles/App.css';

const ProtectedRoute = ({ children, allowedRoles }) => {
    const { user, loading } = useAuth();

    if (loading) {
        return (
            <div className="loading-container">
                <div className="loading-content">
                    <div className="loading-icon">⚡</div>
                    <p>Loading...</p>
                </div>
            </div>
        );
    }

    // check if user is authenticated
    if (!user) {
        return <Navigate to="/login" replace />;
    }

    // check if user has the required role
    if (allowedRoles && !allowedRoles.includes(user.role)) {
        return <Navigate to="/login" replace />;
    }

    // if the checks pass, render the protected component
    return children;
};

function App() {
    return (
        // the AuthProvider makes auth state available throughout the app
        <AuthProvider>
            <WebSocketProvider>
                <Router>
                    <Routes>
                        {/*public routes*/}
                        <Route path="/" element={<Navigate to="/login" replace />} />
                        <Route path="/login" element={<Login />} />
                        {/*<Route path="/register" element={<Register />} />*/}
                        {/*protected routes*/}
                        <Route path="/admin"
                            element={
                                <ProtectedRoute allowedRoles={['ADMIN']}>
                                    <AdminDashboard />
                                </ProtectedRoute>
                            }
                        />
                        <Route path="/client"
                            element={
                                <ProtectedRoute allowedRoles={['CLIENT']}>
                                    <ClientDashboard />
                                </ProtectedRoute>
                            }
                        />
                        {/*catch-all route to redirect unknown paths to login*/}
                        <Route path="*" element={<Navigate to="/login" replace />} />
                    </Routes>
                </Router>
            </WebSocketProvider>
        </AuthProvider>
    );
}

export default App;