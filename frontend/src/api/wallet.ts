import client from './client';

export interface WalletBalance {
    currency: string;
    available: number;
    reserved: number;
    total: number;
}

export interface FaucetResponse {
    message: string;
    balance: WalletBalance;
}

export const walletApi = {
    // Fixed: Backend uses /balances (plural), not /balance
    getBalance: async (userId: string): Promise<WalletBalance[]> => {
        const response = await client.get<WalletBalance[]>(`/wallet/${userId}/balances`);
        return response.data;
    },

    // Fixed: Backend expects userId as path param, not in body
    faucet: async (userId: string): Promise<FaucetResponse> => {
        const response = await client.post<FaucetResponse>(`/wallet/${userId}/faucet`);
        return response.data;
    }
};
