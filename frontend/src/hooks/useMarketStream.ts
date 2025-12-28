import { useState, useEffect, useCallback, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import type { IMessage, StompSubscription } from '@stomp/stompjs';

// Matches backend TickerResponse WebSocket broadcast format
export interface TickerData {
    symbol: string;
    coinId?: string;
    price: string;
    change24h: string;           // Backend field name (was priceChange)
    changePercent24h: string;    // Backend field name (was priceChangePercent)
    // Aliased for backward compatibility with UI components
    priceChange?: string;
    priceChangePercent?: string;
    high24h: string;
    low24h: string;
    volume24h: string;
    timestamp: string;
    stale?: boolean;
}

export interface CandleData {
    time: string;
    open: number;
    high: number;
    low: number;
    close: number;
    volume?: number;
}

interface UseMarketStreamOptions {
    symbol?: string;
    autoConnect?: boolean;
}

interface UseMarketStreamReturn {
    latestPrice: number | null;
    tickerData: TickerData | null;
    latestCandle: CandleData | null;
    isConnected: boolean;
    error: string | null;
    connect: () => void;
    disconnect: () => void;
}

// WebSocket URL must match backend registration at /ws/market
const GATEWAY_WS_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws/market';
const RECONNECT_DELAY = 5000;
const MAX_RECONNECT_ATTEMPTS = 10;

export const useMarketStream = (options: UseMarketStreamOptions = {}): UseMarketStreamReturn => {
    const { symbol = 'BTCUSDT', autoConnect = true } = options;

    const [latestPrice, setLatestPrice] = useState<number | null>(null);
    const [tickerData, setTickerData] = useState<TickerData | null>(null);
    const [latestCandle, setLatestCandle] = useState<CandleData | null>(null);
    const [isConnected, setIsConnected] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const clientRef = useRef<Client | null>(null);
    const subscriptionRef = useRef<StompSubscription | null>(null);
    const reconnectAttemptsRef = useRef(0);
    const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const handleMessage = useCallback((message: IMessage) => {
        try {
            const data = JSON.parse(message.body);

            // Handle ticker data
            if (data.price) {
                const priceValue = parseFloat(data.price);
                setLatestPrice(priceValue);

                // Map backend fields to aliased fields for UI compatibility
                const tickerWithAliases: TickerData = {
                    ...data,
                    priceChange: data.change24h,
                    priceChangePercent: data.changePercent24h,
                };
                setTickerData(tickerWithAliases);

                // Create candle from ticker (simplified - real implementation would aggregate)
                if (data.timestamp) {
                    const candle: CandleData = {
                        time: data.timestamp.split('T')[0], // Daily candle
                        open: priceValue,
                        high: parseFloat(data.high24h || data.price),
                        low: parseFloat(data.low24h || data.price),
                        close: priceValue,
                        volume: parseFloat(data.volume24h || '0')
                    };
                    setLatestCandle(candle);
                }
            }
        } catch (err) {
            console.error('Error parsing WebSocket message:', err);
        }
    }, []);

    const connect = useCallback(() => {
        if (clientRef.current?.connected) {
            return;
        }

        const client = new Client({
            brokerURL: GATEWAY_WS_URL,
            reconnectDelay: 0, // We handle reconnection manually
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            debug: (str) => {
                if (import.meta.env.DEV) {
                    console.log('[STOMP Debug]', str);
                }
            },
            onConnect: () => {
                console.log('[WebSocket] Connected to', GATEWAY_WS_URL);
                setIsConnected(true);
                setError(null);
                reconnectAttemptsRef.current = 0;

                // Subscribe to market data topic (lowercase to match backend broadcaster)
                subscriptionRef.current = client.subscribe(
                    `/topic/ticker/${symbol.toLowerCase()}`,
                    handleMessage
                );
            },
            onDisconnect: () => {
                console.log('[WebSocket] Disconnected');
                setIsConnected(false);
            },
            onStompError: (frame) => {
                console.error('[WebSocket] STOMP Error:', frame.headers['message']);
                setError(frame.headers['message'] || 'Connection error');
                scheduleReconnect();
            },
            onWebSocketError: (event) => {
                console.error('[WebSocket] WebSocket Error:', event);
                setError('WebSocket connection failed');
                scheduleReconnect();
            },
            onWebSocketClose: () => {
                console.log('[WebSocket] Connection closed');
                setIsConnected(false);
                scheduleReconnect();
            }
        });

        clientRef.current = client;
        client.activate();
    }, [symbol, handleMessage]);

    const scheduleReconnect = useCallback(() => {
        if (reconnectAttemptsRef.current >= MAX_RECONNECT_ATTEMPTS) {
            console.error('[WebSocket] Max reconnection attempts reached');
            setError('Unable to connect. Please refresh the page.');
            return;
        }

        if (reconnectTimeoutRef.current) {
            clearTimeout(reconnectTimeoutRef.current);
        }

        reconnectAttemptsRef.current += 1;
        const delay = RECONNECT_DELAY * reconnectAttemptsRef.current;

        console.log(`[WebSocket] Reconnecting in ${delay}ms (attempt ${reconnectAttemptsRef.current})`);

        reconnectTimeoutRef.current = setTimeout(() => {
            connect();
        }, delay);
    }, [connect]);

    const disconnect = useCallback(() => {
        if (reconnectTimeoutRef.current) {
            clearTimeout(reconnectTimeoutRef.current);
        }

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
        if (autoConnect) {
            connect();
        }

        return () => {
            disconnect();
        };
    }, [autoConnect, connect, disconnect]);

    // Resubscribe when symbol changes
    useEffect(() => {
        if (clientRef.current?.connected && subscriptionRef.current) {
            subscriptionRef.current.unsubscribe();
            subscriptionRef.current = clientRef.current.subscribe(
                `/topic/ticker/${symbol.toLowerCase()}`,
                handleMessage
            );
        }
    }, [symbol, handleMessage]);

    return {
        latestPrice,
        tickerData,
        latestCandle,
        isConnected,
        error,
        connect,
        disconnect
    };
};
