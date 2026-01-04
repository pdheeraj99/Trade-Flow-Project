import { Client } from '@stomp/stompjs';
import type { StompSubscription } from '@stomp/stompjs';
import { getAccessToken } from '../utils/tokens';

interface PendingSubscription {
    destination: string;
    callback: (message: any) => void;
}

class WebSocketService {
    private client: Client;
    private subscriptions: Map<string, StompSubscription> = new Map();
    private pendingSubscriptions: PendingSubscription[] = [];
    private isConnected = false;

    constructor() {
        const wsBase = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws/market';
        this.client = new Client({
            brokerURL: wsBase, // Gateway WebSocket endpoint (must match backend /ws/market)
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            // Attach token in WebSocket protocol headers so gateway can authenticate the upgrade
            webSocketFactory: () => {
                const token = getAccessToken();
                return new WebSocket(wsBase, token ? ['Bearer', token] : undefined);
            },
            onConnect: () => {
                console.log('Connected to WebSocket');
                this.isConnected = true;
                // Apply any pending subscriptions
                this.processPendingSubscriptions();
            },
            onDisconnect: () => {
                console.log('Disconnected from WebSocket');
                this.isConnected = false;
                this.subscriptions.clear();
            },
            onStompError: (frame) => {
                console.error('Broker reported error: ' + frame.headers['message']);
                console.error('Additional details: ' + frame.body);
            },
        });
    }

    private processPendingSubscriptions(): void {
        while (this.pendingSubscriptions.length > 0) {
            const pending = this.pendingSubscriptions.shift();
            if (pending) {
                this.doSubscribe(pending.destination, pending.callback);
            }
        }
    }

    private doSubscribe(destination: string, callback: (message: any) => void): void {
        if (this.subscriptions.has(destination)) {
            // Already subscribed
            return;
        }

        const subscription = this.client.subscribe(destination, (message) => {
            try {
                const body = JSON.parse(message.body);
                callback(body);
            } catch (e) {
                console.error('Failed to parse message body', e);
            }
        });

        this.subscriptions.set(destination, subscription);
    }

    public connect(): void {
        if (!this.client.active) {
            this.client.activate();
        }
    }

    public disconnect(): void {
        if (this.client.active) {
            this.client.deactivate();
        }
    }

    public subscribe(destination: string, callback: (message: any) => void): void {
        if (this.isConnected && this.client.connected) {
            // Already connected, subscribe immediately
            this.doSubscribe(destination, callback);
        } else {
            // Not connected yet, queue the subscription
            console.log(`WebSocket not ready. Queuing subscription to ${destination}`);
            this.pendingSubscriptions.push({ destination, callback });
        }
    }

    public unsubscribe(destination: string): void {
        const subscription = this.subscriptions.get(destination);
        if (subscription) {
            subscription.unsubscribe();
            this.subscriptions.delete(destination);
        }
    }
}

export const webSocketService = new WebSocketService();

