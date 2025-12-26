import React, { useEffect, useState, useRef } from 'react';
import clsx from 'clsx';
import styles from './OrderBook.module.css';

interface PriceLevel {
    price: number;
    quantity: number;
    total: number;
}

interface OrderBookProps {
    bids: PriceLevel[];
    asks: PriceLevel[];
    symbol: string;
    onPriceClick: (price: number) => void;
}

const OrderBook: React.FC<OrderBookProps> = ({ bids, asks, symbol, onPriceClick }) => {
    const [maxTotal, setMaxTotal] = useState(0);
    const [flashingPrices, setFlashingPrices] = useState<Map<number, 'up' | 'down'>>(new Map());
    const prevPricesRef = useRef<Map<number, number>>(new Map());

    useEffect(() => {
        const bidMax = bids.length > 0 ? Math.max(...bids.map(b => b.total)) : 0;
        const askMax = asks.length > 0 ? Math.max(...asks.map(a => a.total)) : 0;
        setMaxTotal(Math.max(bidMax, askMax));
    }, [bids, asks]);

    // Track price changes for flash animation
    useEffect(() => {
        const newFlashing = new Map<number, 'up' | 'down'>();
        const allLevels = [...bids, ...asks];

        allLevels.forEach(level => {
            const prevQty = prevPricesRef.current.get(level.price);
            if (prevQty !== undefined && prevQty !== level.quantity) {
                newFlashing.set(level.price, level.quantity > prevQty ? 'up' : 'down');
            }
        });

        if (newFlashing.size > 0) {
            setFlashingPrices(newFlashing);
            setTimeout(() => setFlashingPrices(new Map()), 500);
        }

        // Update previous prices ref
        const newPrevPrices = new Map<number, number>();
        allLevels.forEach(level => newPrevPrices.set(level.price, level.quantity));
        prevPricesRef.current = newPrevPrices;
    }, [bids, asks]);

    const renderRow = (level: PriceLevel, type: 'bid' | 'ask') => {
        const widthPercentage = maxTotal > 0 ? (level.total / maxTotal) * 100 : 0;
        const isBid = type === 'bid';
        const flashState = flashingPrices.get(level.price);

        return (
            <div
                key={level.price}
                className={clsx(
                    styles.row,
                    flashState === 'up' && styles.flashUp,
                    flashState === 'down' && styles.flashDown
                )}
                onClick={() => onPriceClick(level.price)}
            >
                {/* Depth Bar Background */}
                <div
                    className={clsx(
                        styles.depthBar,
                        isBid ? styles.depthBarBid : styles.depthBarAsk
                    )}
                    style={{ width: `${widthPercentage}%` }}
                />

                {/* Price */}
                <div className={clsx(
                    styles.cell,
                    isBid ? styles.textBuy : styles.textSell
                )}>
                    {level.price.toFixed(2)}
                </div>

                {/* Quantity */}
                <div className={styles.cellQuantity}>
                    {level.quantity.toFixed(5)}
                </div>

                {/* Total */}
                <div className={styles.cellTotal}>
                    {level.total.toFixed(5)}
                </div>
            </div>
        );
    };

    const spreadPrice = bids.length > 0 && asks.length > 0
        ? (asks[0].price - bids[0].price).toFixed(2)
        : '---';

    const spreadPercent = bids.length > 0 && asks.length > 0
        ? (((asks[0].price - bids[0].price) / asks[0].price) * 100).toFixed(3)
        : '---';

    return (
        <div className={styles.container}>
            {/* Header */}
            <div className={styles.header}>
                <div>Price</div>
                <div className={styles.headerRight}>Amount</div>
                <div className={styles.headerRight}>Total</div>
            </div>

            {/* Asks (Sells) - Reversed for visual display */}
            <div className={clsx(styles.scrollable, styles.scrollableReverse)}>
                {asks.slice(0, 12).map(ask => renderRow(ask, 'ask'))}
            </div>

            {/* Spread Indicator */}
            <div className={styles.spreadContainer}>
                <span className={styles.spreadPrice}>
                    {bids.length > 0 ? bids[0].price.toFixed(2) : '---'}
                </span>
                <span className={styles.spreadDetails}>
                    Spread: <span className={styles.spreadValue}>{spreadPrice}</span> ({spreadPercent}%)
                </span>
            </div>

            {/* Bids (Buys) */}
            <div className={styles.scrollable}>
                {bids.slice(0, 12).map(bid => renderRow(bid, 'bid'))}
            </div>
        </div>
    );
};

export default OrderBook;
