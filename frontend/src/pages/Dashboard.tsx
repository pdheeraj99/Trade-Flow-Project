import React, { useEffect, useState, useCallback } from 'react';
import Navbar from '../components/layout/Navbar';
import TradingChart from '../components/trading/TradingChart';
import OrderBook from '../components/trading/OrderBook';
import OrderForm from '../components/trading/OrderForm';
import { marketApi } from '../api/market';
import type { Ticker } from '../api/market';
import { webSocketService } from '../api/websocket';
import type { CandlestickData } from 'lightweight-charts';
import { useAuth } from '../context/AuthContext';
import { walletApi } from '../api/wallet';
import { ordersApi } from '../api/orders';
import Wallet from '../components/trading/Wallet';
import ActiveOrders from '../components/trading/ActiveOrders';

// Default data for chart (placeholder)
const initialChartData: CandlestickData[] = [
    { time: '2023-12-01', open: 42000, high: 43000, low: 41500, close: 42500 },
    { time: '2023-12-02', open: 42500, high: 43500, low: 42000, close: 43200 },
    { time: '2023-12-03', open: 43200, high: 44000, low: 42800, close: 43800 },
];

const Dashboard: React.FC = () => {
    const { user } = useAuth();
    const [ticker, setTicker] = useState<Ticker | null>(null);
    const [chartData, setChartData] = useState<CandlestickData[]>(initialChartData);
    const [bids, setBids] = useState<any[]>([]);
    const [asks, setAsks] = useState<any[]>([]);
    const [orders, setOrders] = useState<any[]>([]);
    const [balances, setBalances] = useState<any[]>([]);
    const [tab, setTab] = useState<'ORDERS' | 'WALLET'>('ORDERS');
    const [refreshTrigger, setRefreshTrigger] = useState(0);

    const symbol = 'BTCUSDT'; // Hardcoded for now, could be dynamic

    const fetchData = useCallback(async () => {
        try {
            // Fetch Ticker
            const tickerData = await marketApi.getTicker(symbol);
            setTicker(tickerData);

            // Fetch User Data if logged in
            if (user) {
                const userBalances = await walletApi.getBalance(user.id.toString());
                setBalances(userBalances);

                // Fetch Orders
                const userOrders = await ordersApi.getUserOrders(user.id);
                setOrders(userOrders);
            }
        } catch (error) {
            console.error('Failed to fetch data', error);
        }
    }, [symbol, user, refreshTrigger]);

    useEffect(() => {
        fetchData();
        webSocketService.connect();

        // Subscribe to Ticker
        webSocketService.subscribe(`/topic/ticker/${symbol}`, (msg) => {
            setTicker(msg);
            // Update chart data logic (simplified)
            if (msg.price) {
                const newPrice = parseFloat(msg.price);

                setChartData(prev => {
                    if (prev.length === 0) return prev;

                    const lastCandle = prev[prev.length - 1] as any;
                    const candle = {
                        time: msg.timestamp ? msg.timestamp.split('T')[0] : lastCandle.time,
                        open: lastCandle.close,
                        high: Math.max(lastCandle.close, newPrice),
                        low: Math.min(lastCandle.close, newPrice),
                        close: newPrice
                    };

                    const newData = [...prev];
                    if (newData[newData.length - 1].time === candle.time) {
                        newData[newData.length - 1] = candle;
                    } else {
                        newData.push(candle);
                    }
                    return newData;
                });
            }
        });

        // Subscribe to Order Book
        webSocketService.subscribe(`/topic/orderbook/${symbol}`, (msg) => {
            if (msg.bids) setBids(msg.bids);
            if (msg.asks) setAsks(msg.asks);
        });

        return () => {
            webSocketService.disconnect();
        };
    }, [fetchData, symbol]);

    const handleOrderSubmit = async (side: 'BUY' | 'SELL', type: 'LIMIT' | 'MARKET', price: number, quantity: number) => {
        if (!user) {
            alert('Please login to place orders');
            return;
        }

        try {
            await ordersApi.createOrder({
                userId: user.id,
                symbol,
                side,
                type,
                price: type === 'LIMIT' ? price : undefined,
                quantity
            });
            console.log('Order placed successfully');
            setRefreshTrigger(prev => prev + 1); // Refresh balance & orders
        } catch (error) {
            console.error('Failed to place order', error);
            alert('Failed to place order');
        }
    };

    const usdtBalance = balances.find(b => b.currency === 'USDT' || b.currency === 'USD')?.available || 0;
    const btcBalance = balances.find(b => b.currency === 'BTC')?.available || 0;

    return (
        <div className="min-h-screen bg-background text-foreground flex flex-col">
            <Navbar />

            <div className="flex-1 p-2 grid grid-cols-12 gap-2 h-[calc(100vh-56px)] overflow-hidden">
                {/* Left Column - Chart & Ticker & Bottom Panel (9 col) */}
                <div className="col-span-9 flex flex-col gap-2 h-full">
                    {/* Top: Ticker & Chart (70%) */}
                    <div className="h-[70%] flex flex-col gap-2">
                        {/* Ticker Header */}
                        <div className="h-12 shrink-0 bg-dark-card border border-border rounded flex items-center px-4 justify-between">
                            <div className="flex items-center gap-4">
                                <h1 className="text-xl font-bold text-foreground">BTC/USDT</h1>
                                <span className="text-2xl font-mono font-semibold text-trade-buy">{ticker?.price || '---'}</span>
                                <span className="text-sm font-mono text-dark-muted">${ticker?.price || '---'}</span>
                            </div>
                            <div className="flex gap-8 text-xs">
                                <div>
                                    <div className="text-dark-muted">24h Change</div>
                                    <div className="text-trade-buy text-green-500">+2.45%</div>
                                </div>
                                <div>
                                    <div className="text-dark-muted">24h High</div>
                                    <div className="text-foreground">{ticker?.price || '---'}</div>
                                </div>
                                <div>
                                    <div className="text-dark-muted">24h Volume(BTC)</div>
                                    <div className="text-foreground">{ticker?.volume24h || '---'}</div>
                                </div>
                            </div>
                        </div>

                        {/* Chart */}
                        <div className="flex-1 bg-dark-card border border-border rounded p-2 overflow-hidden relative">
                            <TradingChart data={chartData} symbol={symbol} />
                        </div>
                    </div>

                    {/* Bottom: Tabs (Active Orders / Wallet) (30%) */}
                    <div className="h-[30%] bg-dark-card border border-border rounded flex flex-col overflow-hidden">
                        <div className="flex border-b border-border">
                            <button
                                className={`px-4 py-2 text-sm font-bold ${tab === 'ORDERS' ? 'text-primary border-b-2 border-primary' : 'text-dark-muted hover:text-foreground'}`}
                                onClick={() => setTab('ORDERS')}
                            >
                                Active Orders
                            </button>
                            <button
                                className={`px-4 py-2 text-sm font-bold ${tab === 'WALLET' ? 'text-primary border-b-2 border-primary' : 'text-dark-muted hover:text-foreground'}`}
                                onClick={() => setTab('WALLET')}
                            >
                                Wallet
                            </button>
                        </div>
                        <div className="flex-1 p-0 overflow-hidden">
                            {tab === 'ORDERS' ? (
                                <ActiveOrders orders={orders} />
                            ) : (
                                <Wallet balances={balances} onRefresh={() => setRefreshTrigger(prev => prev + 1)} />
                            )}
                        </div>
                    </div>
                </div>

                {/* Right Column - Order Book & Form (3 col) */}
                <div className="col-span-3 flex flex-col gap-2 h-full">
                    {/* Order Book (60%) */}
                    <div className="h-[60%] bg-dark-card border border-border rounded overflow-hidden">
                        <OrderBook bids={bids} asks={asks} symbol={symbol} onPriceClick={(p) => console.log(p)} />
                    </div>

                    {/* Order Form (40%) */}
                    <div className="h-[40%] bg-dark-card border border-border rounded overflow-hidden">
                        <OrderForm
                            symbol={symbol}
                            baseBalance={btcBalance}
                            quoteBalance={usdtBalance}
                            onSubmit={handleOrderSubmit}
                        />
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;
