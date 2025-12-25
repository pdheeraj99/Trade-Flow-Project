import { Client } from '@stomp/stompjs';
import type { StompSubscription } from '@stomp/stompjs';

class WebSocketService {
    private client: Client;
    private subscriptions: Map<string, StompSubscription> = new Map();

    constructor() {
        this.client = new Client({
            brokerURL: 'ws://localhost:8080/ws', // Gateway WebSocket endpoint
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            onConnect: () => {
                console.log('Connected to WebSocket');
            },
            onDisconnect: () => {
                console.log('Disconnected from WebSocket');
                this.subscriptions.clear();
            },
            onStompError: (frame) => {
                console.error('Broker reported error: ' + frame.headers['message']);
                console.error('Additional details: ' + frame.body);
            },
        });
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
        if (!this.client.connected) {
            console.warn('WebSocket not connected. Subscription might fail immediately but will retry on connect.');
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

    public unsubscribe(destination: string): void {
        const subscription = this.subscriptions.get(destination);
        if (subscription) {
            subscription.unsubscribe();
            this.subscriptions.delete(destination);
        }
    }
}

export const webSocketService = new WebSocketService();
