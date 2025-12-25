import React, { useEffect, useState, useCallback, useMemo } from 'react';
import Navbar from '../components/layout/Navbar';
import TradingChart from '../components/trading/TradingChart';
import OrderBook from '../components/trading/OrderBook';
import OrderForm from '../components/trading/OrderForm';
import { marketApi } from '../api/market';
import { webSocketService } from '../api/websocket';
import type { CandlestickData } from 'lightweight-charts';
import { useAuth } from '../context/AuthContext';
import { walletApi } from '../api/wallet';
import { ordersApi } from '../api/orders';
import Wallet from '../components/trading/Wallet';
import ActiveOrders from '../components/trading/ActiveOrders';
import { useMarketStream } from '../hooks/useMarketStream';
import clsx from 'clsx';

// Initial dummy data for chart history (before backend history API is ready)
const generateInitialChartData = (): CandlestickData[] => {
    const data: CandlestickData[] = [];
    let price = 42000;
    const now = new Date();

    for (let i = 30; i >= 0; i--) {
        const date = new Date(now);
        date.setDate(date.getDate() - i);
        const dateStr = date.toISOString().split('T')[0];

        const change = (Math.random() - 0.5) * 2000;
        const open = price;
        const close = price + change;
        const high = Math.max(open, close) + Math.random() * 500;
        const low = Math.min(open, close) - Math.random() * 500;

        data.push({
            time: dateStr,
            open: Math.round(open * 100) / 100,
            high: Math.round(high * 100) / 100,
            low: Math.round(low * 100) / 100,
            close: Math.round(close * 100) / 100,
        });

        price = close;
    }

    return data;
};

const Dashboard: React.FC = () => {
    const { user } = useAuth();
    const [chartData, setChartData] = useState<CandlestickData[]>(() => generateInitialChartData());
    const [bids, setBids] = useState<any[]>([]);
    const [asks, setAsks] = useState<any[]>([]);
    const [orders, setOrders] = useState<any[]>([]);
    const [balances, setBalances] = useState<any[]>([]);
    const [tab, setTab] = useState<'ORDERS' | 'WALLET'>('ORDERS');
    const [refreshTrigger, setRefreshTrigger] = useState(0);
    const [previousPrice, setPreviousPrice] = useState<number | null>(null);

    const symbol = 'BTCUSDT';

    // Use the market stream hook for real-time data
    const {
        latestPrice,
        tickerData,
        isConnected,
        error: wsError
    } = useMarketStream({ symbol, autoConnect: true });

    // Determine price trend (green/red)
    const priceTrend = useMemo(() => {
        if (latestPrice === null || previousPrice === null) return 'neutral';
        if (latestPrice > previousPrice) return 'up';
        if (latestPrice < previousPrice) return 'down';
        return 'neutral';
    }, [latestPrice, previousPrice]);

    // Track previous price for trend
    useEffect(() => {
        if (latestPrice !== null && latestPrice !== previousPrice) {
            setPreviousPrice(latestPrice);
        }
    }, [latestPrice, previousPrice]);

    // Update chart when new price arrives
    useEffect(() => {
        if (latestPrice === null) return;

        setChartData(prev => {
            if (prev.length === 0) return prev;

            const now = new Date();
            const todayStr = now.toISOString().split('T')[0];
            const lastCandle = prev[prev.length - 1];

            // Update last candle if same day, otherwise add new
            if (lastCandle.time === todayStr) {
                const updatedCandle: CandlestickData = {
                    time: todayStr,
                    open: lastCandle.open,
                    high: Math.max(lastCandle.high as number, latestPrice),
                    low: Math.min(lastCandle.low as number, latestPrice),
                    close: latestPrice,
                };
                return [...prev.slice(0, -1), updatedCandle];
            } else {
                // New day - add new candle
                const newCandle: CandlestickData = {
                    time: todayStr,
                    open: latestPrice,
                    high: latestPrice,
                    low: latestPrice,
                    close: latestPrice,
                };
                return [...prev, newCandle];
            }
        });
    }, [latestPrice]);

    // Fetch user data (wallet, orders) - uses REST API
    const fetchUserData = useCallback(async () => {
        if (!user) return;

        try {
            const userBalances = await walletApi.getBalance(user.id.toString());
            setBalances(userBalances);

            const userOrders = await ordersApi.getUserOrders(user.id);
            setOrders(userOrders);
        } catch (error) {
            console.error('Failed to fetch user data', error);
        }
    }, [user]);

    // Also try to fetch from REST API on mount
    useEffect(() => {
        const fetchInitialTicker = async () => {
            try {
                await marketApi.getTicker(symbol);
            } catch (err) {
                console.log('Initial ticker fetch failed, will use WebSocket');
            }
        };

        fetchInitialTicker();
        fetchUserData();
    }, [fetchUserData, symbol]);

    // Refresh user data when trigger changes
    useEffect(() => {
        if (refreshTrigger > 0) {
            fetchUserData();
        }
    }, [refreshTrigger, fetchUserData]);

    // Subscribe to order book updates (still using the old service for now)
    useEffect(() => {
        webSocketService.connect();

        webSocketService.subscribe(`/topic/orderbook/${symbol}`, (msg) => {
            if (msg.bids) setBids(msg.bids);
            if (msg.asks) setAsks(msg.asks);
        });

        return () => {
            webSocketService.disconnect();
        };
    }, [symbol]);

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
            setRefreshTrigger(prev => prev + 1);
        } catch (error) {
            console.error('Failed to place order', error);
            alert('Failed to place order');
        }
    };

    const usdtBalance = balances.find(b => b.currency === 'USDT' || b.currency === 'USD')?.available || 0;
    const btcBalance = balances.find(b => b.currency === 'BTC')?.available || 0;

    // Format price for display
    const displayPrice = latestPrice !== null
        ? latestPrice.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
        : tickerData?.price || '---';

    return (
        <div className="min-h-screen bg-background text-foreground flex flex-col">
            <Navbar />

            <div className="flex-1 p-2 grid grid-cols-12 gap-2 h-[calc(100vh-56px)] overflow-hidden">
                {/* Left Column - Chart & Ticker & Bottom Panel (9 col) */}
                <div className="col-span-9 flex flex-col gap-2 h-full">
                    {/* Top: Ticker & Chart (70%) */}
                    <div className="h-[70%] flex flex-col gap-2">
                        {/* Ticker Header */}
                        <div className="h-14 shrink-0 bg-dark-card border border-border rounded flex items-center px-4 justify-between">
                            <div className="flex items-center gap-4">
                                <h1 className="text-xl font-bold text-foreground">BTC/USDT</h1>

                                {/* Live Price with Trend Color */}
                                <span className={clsx(
                                    "text-3xl font-mono font-bold transition-colors duration-200",
                                    {
                                        'text-trade-buy': priceTrend === 'up',
                                        'text-trade-sell': priceTrend === 'down',
                                        'text-foreground': priceTrend === 'neutral',
                                    }
                                )}>
                                    ${displayPrice}
                                </span>

                                {/* Connection Status Indicator */}
                                <div className={clsx(
                                    "w-2 h-2 rounded-full",
                                    {
                                        'bg-green-500': isConnected,
                                        'bg-red-500 animate-pulse': !isConnected && wsError,
                                        'bg-yellow-500 animate-pulse': !isConnected && !wsError,
                                    }
                                )} title={isConnected ? 'Connected' : wsError || 'Connecting...'} />
                            </div>

                            <div className="flex gap-8 text-xs">
                                <div>
                                    <div className="text-dark-muted">24h Change</div>
                                    <div className={clsx({
                                        'text-trade-buy': tickerData?.priceChangePercent?.startsWith('+') || (parseFloat(tickerData?.priceChangePercent || '0') > 0),
                                        'text-trade-sell': tickerData?.priceChangePercent?.startsWith('-') || (parseFloat(tickerData?.priceChangePercent || '0') < 0),
                                        'text-foreground': !tickerData?.priceChangePercent,
                                    })}>
                                        {tickerData?.priceChangePercent ? `${tickerData.priceChangePercent}%` : '---'}
                                    </div>
                                </div>
                                <div>
                                    <div className="text-dark-muted">24h High</div>
                                    <div className="text-foreground">{tickerData?.high24h || '---'}</div>
                                </div>
                                <div>
                                    <div className="text-dark-muted">24h Low</div>
                                    <div className="text-foreground">{tickerData?.low24h || '---'}</div>
                                </div>
                                <div>
                                    <div className="text-dark-muted">24h Volume</div>
                                    <div className="text-foreground">{tickerData?.volume24h || '---'}</div>
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
                                className={clsx(
                                    "px-4 py-2 text-sm font-bold transition-colors",
                                    tab === 'ORDERS'
                                        ? 'text-primary border-b-2 border-primary'
                                        : 'text-dark-muted hover:text-foreground'
                                )}
                                onClick={() => setTab('ORDERS')}
                            >
                                Active Orders
                            </button>
                            <button
                                className={clsx(
                                    "px-4 py-2 text-sm font-bold transition-colors",
                                    tab === 'WALLET'
                                        ? 'text-primary border-b-2 border-primary'
                                        : 'text-dark-muted hover:text-foreground'
                                )}
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
