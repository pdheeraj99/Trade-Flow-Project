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
import { useOrderUpdates } from '../hooks/useOrderUpdates';
import clsx from 'clsx';
import styles from './Dashboard.module.css';

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

    // Use order updates hook for real-time order status updates
    const {
        latestUpdate: orderUpdate
    } = useOrderUpdates({ userId: user?.id.toString(), autoConnect: true });

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

            // CRITICAL: Handle both string and number types for lastCandle.time
            // lightweight-charts may have mutated the time to a number (timestamp)
            // Normalize both values to date strings for comparison
            let lastCandleDate: string;
            if (typeof lastCandle.time === 'string') {
                lastCandleDate = lastCandle.time;
            } else {
                // Convert timestamp to YYYY-MM-DD format
                const date = new Date(lastCandle.time as number);
                lastCandleDate = date.toISOString().split('T')[0];
            }

            // Update last candle if same day, otherwise add new
            if (lastCandleDate === todayStr) {
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

            // Fixed: getUserOrders no longer takes userId parameter
            // Backend extracts userId from X-User-Id header automatically
            const userOrders = await ordersApi.getUserOrders();
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

    // Update orders in real-time when order update is received
    useEffect(() => {
        if (!orderUpdate) return;

        setOrders(prevOrders => {
            const index = prevOrders.findIndex(o => o.orderId === orderUpdate.orderId);
            if (index !== -1) {
                // Update existing order
                const updated = [...prevOrders];
                updated[index] = {
                    ...updated[index],
                    status: orderUpdate.status,
                    filledQuantity: orderUpdate.filledQuantity
                };
                return updated;
            }
            // Order not found, trigger full refresh
            setRefreshTrigger(prev => prev + 1);
            return prevOrders;
        });

        // Also refresh wallet balances when order status changes
        if (orderUpdate.status === 'FILLED' || orderUpdate.status === 'PARTIALLY_FILLED') {
            setRefreshTrigger(prev => prev + 1);
        }
    }, [orderUpdate]);

    // Subscribe to order book updates (still using the old service for now)
    useEffect(() => {
        webSocketService.connect();

        webSocketService.subscribe(`/topic/orderbook/${symbol.toLowerCase()}`, (msg) => {
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
            // Fixed: Removed userId from request body
            // Backend extracts userId from X-User-Id header (added by client interceptor)
            await ordersApi.createOrder({
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

    const usdtBalance = balances.find(b => b.currency === 'USDT' || b.currency === 'USD')?.availableBalance || 0;
    const btcBalance = balances.find(b => b.currency === 'BTC')?.availableBalance || 0;

    // Format price for display
    const displayPrice = latestPrice !== null
        ? latestPrice.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
        : tickerData?.price || '---';

    return (
        <div className={styles.dashboardContainer}>
            <Navbar isConnected={isConnected} connectionError={wsError} />

            <div className={styles.gridContent}>
                {/* Left Column - Chart & Ticker & Bottom Panel (9 col) */}
                <div className={styles.leftColumn}>
                    {/* Top: Ticker & Chart (70%) */}
                    <div className={styles.tickerChartSection}>
                        {/* Ticker Header */}
                        <div className={styles.tickerHeader}>
                            <div className={styles.tickerLeft}>
                                <h1 className={styles.tickerSymbol}>BTC/USDT</h1>

                                {/* Live Price with Trend Color */}
                                <span className={clsx(
                                    styles.tickerPrice,
                                    {
                                        [styles.textBuy]: priceTrend === 'up',
                                        [styles.textSell]: priceTrend === 'down',
                                        [styles.textNeutral]: priceTrend === 'neutral',
                                    }
                                )}>
                                    ${displayPrice}
                                </span>

                                {/* Connection Status Indicator */}
                                <div className={clsx(
                                    styles.connectionDot,
                                    {
                                        [styles.dotConnected]: isConnected,
                                        [styles.dotError]: !isConnected && wsError,
                                        [styles.dotConnecting]: !isConnected && !wsError,
                                    }
                                )} title={isConnected ? 'Connected' : wsError || 'Connecting...'} />
                            </div>

                            <div className={styles.tickerStats}>
                                <div className={styles.statItem}>
                                    <div className={styles.statLabel}>24h Change</div>
                                    <div className={clsx({
                                        [styles.textBuy]: tickerData?.changePercent24h != null && Number(tickerData.changePercent24h) > 0,
                                        [styles.textSell]: tickerData?.changePercent24h != null && Number(tickerData.changePercent24h) < 0,
                                        [styles.textNeutral]: tickerData?.changePercent24h == null,
                                    })}>
                                        {tickerData?.changePercent24h != null
                                            ? `${Number(tickerData.changePercent24h) >= 0 ? '+' : ''}${Number(tickerData.changePercent24h).toFixed(2)}%`
                                            : '---'}
                                    </div>
                                </div>
                                <div className={styles.statItem}>
                                    <div className={styles.statLabel}>24h High</div>
                                    <div className={styles.statValue}>{tickerData?.high24h || '---'}</div>
                                </div>
                                <div className={styles.statItem}>
                                    <div className={styles.statLabel}>24h Low</div>
                                    <div className={styles.statValue}>{tickerData?.low24h || '---'}</div>
                                </div>
                                <div className={styles.statItem}>
                                    <div className={styles.statLabel}>24h Volume</div>
                                    <div className={styles.statValue}>{tickerData?.volume24h || '---'}</div>
                                </div>
                            </div>
                        </div>

                        {/* Chart */}
                        <div className={styles.chartContainer}>
                            <TradingChart data={chartData} symbol={symbol} />
                        </div>
                    </div>

                    {/* Bottom: Tabs (Active Orders / Wallet) (30%) */}
                    <div className={styles.tabsSection}>
                        <div className={styles.tabsHeader}>
                            <button
                                className={clsx(
                                    styles.tabButton,
                                    tab === 'ORDERS' && styles.tabButtonActive
                                )}
                                onClick={() => setTab('ORDERS')}
                            >
                                Active Orders
                            </button>
                            <button
                                className={clsx(
                                    styles.tabButton,
                                    tab === 'WALLET' && styles.tabButtonActive
                                )}
                                onClick={() => setTab('WALLET')}
                            >
                                Wallet
                            </button>
                        </div>
                        <div className={styles.tabContent}>
                            {tab === 'ORDERS' ? (
                                <ActiveOrders orders={orders} />
                            ) : (
                                <Wallet onRefresh={() => setRefreshTrigger(prev => prev + 1)} />
                            )}
                        </div>
                    </div>
                </div>

                {/* Right Column - Order Book & Form (3 col) */}
                <div className={styles.rightColumn}>
                    {/* Order Book (60%) */}
                    <div className={styles.orderBookSection}>
                        <OrderBook bids={bids} asks={asks} symbol={symbol} onPriceClick={(p) => console.log(p)} />
                    </div>

                    {/* Order Form (40%) */}
                    <div className={styles.orderFormSection}>
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
