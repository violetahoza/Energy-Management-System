import React from 'react';

const LoadingSpinner = ({ message = "Loading..." }) => {
    return (
        <div className="empty-state">
            <p lassName="empty-state-text">{message}</p>
        </div>
    );
};

export default LoadingSpinner;