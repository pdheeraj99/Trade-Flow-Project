import { useState, useEffect, useCallback, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import type { StompSubscription } from '@stomp/stompjs';

export interface OrderUpdateEvent {
    orderId: string;
    userId: string;
    symbol: string;
    status: string;
    filledQuantity: number;
    timestamp: string;
}

interface UseOrderUpdatesOptions {
    userId?: string;
    autoConnect?: boolean;
}

interface UseOrderUpdatesReturn {
    latestUpdate: OrderUpdateEvent | null;
    isConnected: boolean;
    error: string | null;
    connect: () => void;
    disconnect: () => void;
}

const GATEWAY_WS_URL = import.meta.env.VITE_WS_URL_ORDERS || 'ws://localhost:8080/ws/orders';
const RECONNECT_DELAY = 5000;

export const useOrderUpdates = (options: UseOrderUpdatesOptions = {}): UseOrderUpdatesReturn => {
    const { userId, autoConnect = true } = options;

    const [latestUpdate, setLatestUpdate] = useState<OrderUpdateEvent | null>(null);
    const [isConnected, setIsConnected] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const clientRef = useRef<Client | null>(null);
    const subscriptionRef = useRef<StompSubscription | null>(null);

    const connect = useCallback(() => {
        if (!userId || clientRef.current?.connected) {
            return;
        }

        const client = new Client({
            brokerURL: GATEWAY_WS_URL,
            reconnectDelay: RECONNECT_DELAY,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            debug: (str) => {
                if (import.meta.env.DEV) {
                    console.log('[Order WS Debug]', str);
                }
            },
            onConnect: () => {
                console.log('[Order WebSocket] Connected');
                setIsConnected(true);
                setError(null);

                // Subscribe to user-specific order updates
                subscriptionRef.current = client.subscribe(
                    `/topic/orders/${userId}`,
                    (message) => {
                        try {
                            const update = JSON.parse(message.body);
                            setLatestUpdate(update);
                        } catch (err) {
                            console.error('Error parsing order update:', err);
                        }
                    }
                );
            },
            onDisconnect: () => {
                console.log('[Order WebSocket] Disconnected');
                setIsConnected(false);
            },
            onStompError: (frame) => {
                console.error('[Order WebSocket] STOMP Error:', frame.headers['message']);
                setError(frame.headers['message'] || 'Connection error');
            },
            onWebSocketError: (event) => {
                console.error('[Order WebSocket] Error:', event);
                setError('WebSocket connection failed');
            }
        });

        clientRef.current = client;
        client.activate();
    }, [userId]);

    const disconnect = useCallback(() => {
        if (subscriptionRef.current) {
            subscriptionRef.current.unsubscribe();
            subscriptionRef.current = null;
        }

        if (clientRef.current) {
            clientRef.current.deactivate();
            clientRef.current = null;
        }

        setIsConnected(false);
    }, []);

    useEffect(() => {
        if (autoConnect && userId) {
            connect();
        }

        return () => {
            disconnect();
        };
    }, [autoConnect, userId, connect, disconnect]);

    return {
        latestUpdate,
        isConnected,
        error,
        connect,
        disconnect
    };
};
