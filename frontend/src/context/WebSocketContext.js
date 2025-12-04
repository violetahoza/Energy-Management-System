import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import WebSocketService from '../services/websocket';
import { useAuth } from './AuthContext';

const WebSocketContext = createContext(null);

export const WebSocketProvider = ({ children }) => {
    const { user } = useAuth();
    const [isConnected, setIsConnected] = useState(false);
    const webSocketService = useRef(new WebSocketService()).current;

    useEffect(() => {
        const onConnect = () => setIsConnected(true);
        const onDisconnect = () => setIsConnected(false);

        webSocketService.subscribe('connect', onConnect);
        webSocketService.subscribe('disconnect', onDisconnect);

        if (user) {
            const token = localStorage.getItem('token');
            webSocketService.connect(user.userId, token, user.role === 'ADMIN');
        } else {
            webSocketService.disconnect();
        }

        return () => {
            webSocketService.unsubscribe('connect', onConnect);
            webSocketService.unsubscribe('disconnect', onDisconnect);
            webSocketService.disconnect();
        };
    }, [user, webSocketService]);

    const contextValue = {
        isConnected,
        connect: (userId, token, isAdmin) => webSocketService.connect(userId, token, isAdmin),
        disconnect: () => webSocketService.disconnect(),
        subscribe: (event, cb) => webSocketService.subscribe(event, cb),
        unsubscribe: (event, cb) => webSocketService.unsubscribe(event, cb),
        sendMessage: (content) => webSocketService.sendMessage(content),
        sendAdminResponse: (userId, content) => webSocketService.sendAdminResponse(userId, content)
    };

    return (
        <WebSocketContext.Provider value={contextValue}>
            {children}
        </WebSocketContext.Provider>
    );
};

export const useWebSocket = () => {
    return useContext(WebSocketContext);
};