import client from './client';

export interface WalletBalance {
    walletId: string;
    userId: string;
    currency: string;
    availableBalance: number;
    reservedBalance: number;
    // Computed on frontend since backend getter may not serialize
    available: number;
    reserved: number;
    total: number;
}

// Transform backend response to match expected frontend shape
export const transformWalletBalance = (raw: any): WalletBalance => ({
    walletId: raw.walletId,
    userId: raw.userId,
    currency: raw.currency,
    availableBalance: raw.availableBalance ?? 0,
    reservedBalance: raw.reservedBalance ?? 0,
    // Map to simplified field names for UI components
    available: raw.availableBalance ?? 0,
    reserved: raw.reservedBalance ?? 0,
    // Use backend totalBalance if available, otherwise compute
    total: raw.totalBalance ?? ((raw.availableBalance ?? 0) + (raw.reservedBalance ?? 0)),
});

export interface FaucetResponse {
    message: string;
    balance: WalletBalance;
}

export const walletApi = {
    // Fixed: Backend uses /balances (plural), not /balance
    // Transform response to include computed fields (available, reserved, total)
    getBalance: async (userId: string): Promise<WalletBalance[]> => {
        const response = await client.get<any[]>(`/wallet/${userId}/balances`);
        return response.data.map(transformWalletBalance);
    },

    // Fixed: Backend expects userId as path param, not in body
    faucet: async (userId: string): Promise<FaucetResponse> => {
        const response = await client.post<any>(`/wallet/${userId}/faucet`);
        return {
            message: response.data.message,
            balance: transformWalletBalance(response.data.balance),
        };
    }
};
