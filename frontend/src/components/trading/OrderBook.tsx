import React, { useEffect, useState } from 'react';
import clsx from 'clsx';

interface PriceLevel {
    price: number;
    quantity: number;
    total: number; // Cumulative total
}

interface OrderBookProps {
    bids: PriceLevel[]; // Buy side (descending)
    asks: PriceLevel[]; // Sell side (ascending)
    symbol: string;
    onPriceClick: (price: number) => void;
}

const OrderBook: React.FC<OrderBookProps> = ({ bids, asks, symbol, onPriceClick }) => {
    const [maxTotal, setMaxTotal] = useState(0);

    useEffect(() => {
        // Calculate max total for depth visualization
        const bidMax = bids.length > 0 ? bids[bids.length - 1].total : 0;
        const askMax = asks.length > 0 ? asks[asks.length - 1].total : 0;
        setMaxTotal(Math.max(bidMax, askMax));
    }, [bids, asks]);

    const renderRow = (level: PriceLevel, type: 'bid' | 'ask') => {
        const widthPercentage = maxTotal > 0 ? (level.total / maxTotal) * 100 : 0;
        const bgClass = type === 'bid' ? 'bg-trade-buyBg' : 'bg-trade-sellBg';
        const textClass = type === 'bid' ? 'text-trade-buy' : 'text-trade-sell';

        return (
            <div
                key={level.price}
                className="relative grid grid-cols-3 text-xs py-0.5 cursor-pointer hover:bg-dark-hover"
                onClick={() => onPriceClick(level.price)}
            >
                <div
                    className={clsx("absolute top-0 right-0 h-full opacity-20", bgClass)}
                    style={{ width: `${widthPercentage}%` }}
                />
                <div className={clsx("pl-2 z-10 font-mono", textClass)}>{level.price.toFixed(2)}</div>
                <div className="text-right z-10 font-mono text-gray-300">{level.quantity.toFixed(4)}</div>
                <div className="text-right pr-2 z-10 font-mono text-gray-400">{level.total.toFixed(4)}</div>
            </div>
        );
    };

    return (
        <div className="flex flex-col h-full bg-dark-card rounded border border-border overflow-hidden">
            <div className="grid grid-cols-3 text-xs text-dark-muted px-2 py-1 border-b border-border font-semibold">
                <div>Price ({symbol.split('USDT')[0]})</div>
                <div className="text-right">Amount ({symbol.split('USDT')[0]})</div>
                <div className="text-right">Total</div>
            </div>

            {/* Asks (Sells) - Red - Reverse order (Highest to Lowest visually for standard view, but here we stack) */}
            <div className="flex-1 overflow-y-auto flex flex-col-reverse">
                {asks.slice(0, 15).map(ask => renderRow(ask, 'ask'))}
            </div>

            <div className="py-2 border-y border-border text-center text-lg font-bold text-foreground">
                {bids.length > 0 ? bids[0].price.toFixed(2) : '---'}
            </div>

            {/* Bids (Buys) - Green */}
            <div className="flex-1 overflow-y-auto">
                {bids.slice(0, 15).map(bid => renderRow(bid, 'bid'))}
            </div>
        </div>
    );
};

export default OrderBook;
