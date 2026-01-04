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
    availableBalance: raw.availableBalance ? parseFloat(raw.availableBalance) : 0,
    reservedBalance: raw.reservedBalance ? parseFloat(raw.reservedBalance) : 0,
    // Map to simplified field names for UI components
    available: raw.availableBalance ? parseFloat(raw.availableBalance) : 0,
    reserved: raw.reservedBalance ? parseFloat(raw.reservedBalance) : 0,
    // Use backend totalBalance if available, otherwise compute
    total: raw.totalBalance ? parseFloat(raw.totalBalance) :
        ((raw.availableBalance ? parseFloat(raw.availableBalance) : 0) +
            (raw.reservedBalance ? parseFloat(raw.reservedBalance) : 0)),
});

export interface FaucetResponse {
    message: string;
    balance: WalletBalance;
}

export const walletApi = {
    // Get balances for authenticated user (userId inferred from JWT)
    getBalance: async (): Promise<WalletBalance[]> => {
        const response = await client.get<any[]>(`/wallet/balances`);
        return response.data.map(transformWalletBalance);
    },

    // Claim faucet for authenticated user
    faucet: async (): Promise<FaucetResponse> => {
        const response = await client.post<any>(`/wallet/faucet`);
        return {
            message: response.data.message,
            balance: transformWalletBalance(response.data.balance),
        };
    }
};
