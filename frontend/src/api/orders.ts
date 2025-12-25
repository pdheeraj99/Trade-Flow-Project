import client from './client';

export interface Order {
    orderId: string;
    userId: number;
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
    userId: number;
    symbol: string;
    side: 'BUY' | 'SELL';
    type: 'LIMIT' | 'MARKET';
    price?: number; // Optional for MARKET orders
    quantity: number;
}

export const ordersApi = {
    createOrder: async (order: CreateOrderRequest): Promise<Order> => {
        const response = await client.post<Order>('/orders', order);
        return response.data;
    },

    getUserOrders: async (userId: number): Promise<Order[]> => {
        const response = await client.get<Order[]>(`/orders/user/${userId}`);
        return response.data;
    }
};
