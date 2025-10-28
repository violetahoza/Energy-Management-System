import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import '../../styles/App.css';
import Alert from "../common/Alert";

const Login = () => {
    const navigate = useNavigate();
    const { login } = useAuth();
    const [formData, setFormData] = useState({
        username: '',
        password: ''
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
        setError('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const user = await login(formData.username, formData.password);

            if (user.role === 'ADMIN') {
                navigate('/admin');
            } else {
                navigate('/client');
            }
        } catch (err) {
            setError(err.message || 'Invalid username or password');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-container">
            <div className="auth-card">
                <div className="auth-header">
                    <div className="auth-logo">
                        <div className="auth-logo-icon">⚡</div>
                        <h1 className="auth-title">Energy Management</h1>
                    </div>
                    <p className="auth-subtitle">Sign in to your account</p>
                </div>

                {error && <Alert type="error" message={error} onClose={() => setError('')} />}

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label className="form-label" htmlFor="username"> Username </label>
                        <input type="text" id="username" name="username" className="form-input" placeholder="Enter your username" value={formData.username} onChange={handleChange} required autoFocus/>
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="password"> Password </label>
                        <input type="password" id="password" name="password" className="form-input" placeholder="Enter your password" value={formData.password} onChange={handleChange} required/>
                    </div>

                    <button type="submit" className="btn btn-primary" disabled={loading}>
                        {loading ? (
                            <>
                                <span>⏳</span>
                                <span>Signing in...</span>
                            </>
                        ) : (
                            <>
                                <span>🔐</span>
                                <span>Sign In</span>
                            </>
                        )}
                    </button>
                </form>

                <div className="auth-footer">
                    Don't have an account?{' '}
                    <Link to="/register" className="auth-link"> Sign up </Link>
                </div>
            </div>
        </div>
    );
};

export default Login;