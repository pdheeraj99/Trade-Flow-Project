import client from './client';

// Matches backend TickerResponse.java exactly
export interface Ticker {
    symbol: string;
    coinId: string;
    price: string;
    change24h: string | null;        // Backend field name (was price24h)
    changePercent24h: string | null;
    high24h: string | null;
    low24h: string | null;
    volume24h: string | null;
    timestamp: string;
    stale: boolean;
}

export const marketApi = {
    getTicker: async (symbol: string): Promise<Ticker> => {
        const response = await client.get<Ticker>(`/market/ticker/${symbol}`);
        return response.data;
    },

    getAllTickers: async (): Promise<Ticker[]> => {
        const response = await client.get<Ticker[]>('/market/tickers');
        return response.data;
    }
};
