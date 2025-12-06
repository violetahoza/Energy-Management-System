import React from 'react';

const Alert = ({ type = 'error', message, onClose, children }) => {
    const icons = {
        error: '⚠',
        success: '✓',
        info: 'ℹ',
        warning: '⚠'
    };

    const content = message || children;

    if (!content) return null;

    return (
        <div className={`alert alert-${type}`}>
            <div className="alert-content">
                <span className="alert-icon">{icons[type]}</span>
                <span>{content}</span>
            </div>
            {onClose && (<button onClick={onClose} className="alert-close" aria-label="Close alert">✕</button>)}
        </div>
    );
};

export default Alert;