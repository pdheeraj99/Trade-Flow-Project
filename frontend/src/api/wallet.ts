import client from './client';

export interface WalletBalance {
    currency: string;
    available: number;
    reserved: number;
    total: number;
}

export const walletApi = {
    getBalance: async (userId: string): Promise<WalletBalance[]> => {
        const response = await client.get<WalletBalance[]>(`/wallet/${userId}/balance`);
        return response.data;
    },

    faucet: async (userId: string): Promise<void> => {
        await client.post('/wallet/faucet', { userId });
    }
};
