import client from './client';

export interface Order {
    orderId: string;
    userId: string; // Changed from number to string (UUID from backend)
    symbol: string;
    side: 'BUY' | 'SELL';
    type: 'LIMIT' | 'MARKET';
    price?: number;
    quantity: number;
    status: string;
    filledQuantity: number;
    createdAt: string;
}

export interface CreateOrderRequest {
    // userId removed - backend expects it in X-User-Id header, not request body
    symbol: string;
    side: 'BUY' | 'SELL';
    type: 'LIMIT' | 'MARKET';
    price?: number; // Optional for MARKET orders
    quantity: number;
}

export const ordersApi = {
    createOrder: async (order: CreateOrderRequest): Promise<Order> => {
        // X-User-Id header is added automatically by client interceptor
        const response = await client.post<Order>('/orders', order);
        return response.data;
    },

    /**
     * Get user's orders with pagination
     * Backend returns Page<OrderResponse>, so we extract the content array
     */
    getUserOrders: async (): Promise<Order[]> => {
        // Backend returns Spring Page object: { content: [...], totalPages, totalElements, ... }
        const response = await client.get<{ content: Order[] }>('/orders');
        return response.data.content ?? [];
    },

    /**
     * Get user's open orders only
     * Backend returns List<OrderResponse> (no pagination wrapper)
     */
    getOpenOrders: async (): Promise<Order[]> => {
        const response = await client.get<Order[]>('/orders/open');
        return response.data;
    },

    /**
     * Cancel an order by ID
     */
    cancelOrder: async (orderId: string): Promise<void> => {
        await client.delete(`/orders/${orderId}`);
    }
};
