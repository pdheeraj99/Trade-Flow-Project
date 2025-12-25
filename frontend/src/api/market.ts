import client from './client';

export interface Ticker {
    symbol: string;
    price: string;
    price24h: string | null;
    volume24h: string | null;
    timestamp: string;
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
