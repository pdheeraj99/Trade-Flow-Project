import React, { useState, useMemo } from 'react';
import clsx from 'clsx';

interface OrderFormProps {
    symbol: string;
    baseBalance: number;
    quoteBalance: number;
    onSubmit: (side: 'BUY' | 'SELL', type: 'LIMIT' | 'MARKET', price: number, quantity: number) => void;
}

const PERCENTAGES = [25, 50, 75, 100] as const;

const OrderForm: React.FC<OrderFormProps> = ({ symbol, baseBalance, quoteBalance, onSubmit }) => {
    const [side, setSide] = useState<'BUY' | 'SELL'>('BUY');
    const [type, setType] = useState<'LIMIT' | 'MARKET'>('LIMIT');
    const [price, setPrice] = useState('');
    const [quantity, setQuantity] = useState('');
    const [selectedPercent, setSelectedPercent] = useState<number | null>(null);

    const baseCurrency = symbol.replace('USDT', '');
    const quoteCurrency = 'USDT';

    const availableBalance = side === 'BUY' ? quoteBalance : baseBalance;
    const displayCurrency = side === 'BUY' ? quoteCurrency : baseCurrency;

    // Calculate estimated total
    const estimatedTotal = useMemo(() => {
        const qty = parseFloat(quantity) || 0;
        const prc = parseFloat(price) || 0;
        return type === 'LIMIT' ? qty * prc : qty * prc;
    }, [quantity, price, type]);

    const handlePercentClick = (percent: number) => {
        setSelectedPercent(percent);
        const fraction = percent / 100;

        if (side === 'BUY') {
            // For buy: calculate quantity based on USDT balance and price
            const priceVal = parseFloat(price) || 0;
            if (priceVal > 0) {
                const maxQty = (quoteBalance * fraction) / priceVal;
                setQuantity(maxQty.toFixed(6));
            }
        } else {
            // For sell: use fraction of base currency balance
            const qty = baseBalance * fraction;
            setQuantity(qty.toFixed(6));
        }
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!quantity || (type === 'LIMIT' && !price)) return;
        onSubmit(side, type, Number(price), Number(quantity));
        setQuantity('');
        setSelectedPercent(null);
    };

    return (
        <div className="flex flex-col h-full bg-dark-card p-3">
            {/* Buy/Sell Toggle */}
            <div className="flex gap-1 mb-3">
                <button
                    className={clsx(
                        "flex-1 py-2 rounded font-bold text-sm transition-all",
                        side === 'BUY'
                            ? "btn-buy"
                            : "bg-dark-hover text-dark-muted hover:text-dark-text"
                    )}
                    onClick={() => { setSide('BUY'); setSelectedPercent(null); }}
                >
                    Buy
                </button>
                <button
                    className={clsx(
                        "flex-1 py-2 rounded font-bold text-sm transition-all",
                        side === 'SELL'
                            ? "btn-sell"
                            : "bg-dark-hover text-dark-muted hover:text-dark-text"
                    )}
                    onClick={() => { setSide('SELL'); setSelectedPercent(null); }}
                >
                    Sell
                </button>
            </div>

            {/* Order Type Tabs */}
            <div className="flex gap-4 mb-3 text-xs font-semibold border-b border-dark-border pb-2">
                <button
                    className={clsx(
                        "transition-colors",
                        type === 'LIMIT' ? "text-primary" : "text-dark-muted hover:text-dark-text"
                    )}
                    onClick={() => setType('LIMIT')}
                >
                    Limit
                </button>
                <button
                    className={clsx(
                        "transition-colors",
                        type === 'MARKET' ? "text-primary" : "text-dark-muted hover:text-dark-text"
                    )}
                    onClick={() => setType('MARKET')}
                >
                    Market
                </button>
            </div>

            <form onSubmit={handleSubmit} className="flex flex-col flex-1 gap-2">
                {/* Price Input */}
                {type === 'LIMIT' && (
                    <div>
                        <label className="block text-[10px] text-dark-muted mb-1 uppercase tracking-wide">
                            Price
                        </label>
                        <div className="relative">
                            <input
                                type="number"
                                value={price}
                                onChange={(e) => setPrice(e.target.value)}
                                className="w-full trading-input py-2 px-3 text-right font-mono-data text-dark-text"
                                placeholder="0.00"
                                step="0.01"
                            />
                            <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-dark-muted">
                                {quoteCurrency}
                            </span>
                        </div>
                    </div>
                )}

                {/* Quantity Input */}
                <div>
                    <label className="block text-[10px] text-dark-muted mb-1 uppercase tracking-wide">
                        Amount
                    </label>
                    <div className="relative">
                        <input
                            type="number"
                            value={quantity}
                            onChange={(e) => { setQuantity(e.target.value); setSelectedPercent(null); }}
                            className="w-full trading-input py-2 px-3 text-right font-mono-data text-dark-text"
                            placeholder="0.00"
                            step="0.000001"
                        />
                        <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-dark-muted">
                            {baseCurrency}
                        </span>
                    </div>
                </div>

                {/* Percentage Slider Buttons */}
                <div className="flex gap-1 mt-1">
                    {PERCENTAGES.map((pct) => (
                        <button
                            key={pct}
                            type="button"
                            className={clsx("percent-btn flex-1", selectedPercent === pct && "active")}
                            onClick={() => handlePercentClick(pct)}
                        >
                            {pct}%
                        </button>
                    ))}
                </div>

                {/* Available Balance */}
                <div className="flex justify-between text-[10px] text-dark-muted mt-1">
                    <span>Available</span>
                    <span className="font-mono-data text-dark-text">
                        {availableBalance.toFixed(4)} {displayCurrency}
                    </span>
                </div>

                {/* Estimated Total */}
                {type === 'LIMIT' && estimatedTotal > 0 && (
                    <div className="flex justify-between text-[10px] text-dark-muted">
                        <span>Total</span>
                        <span className="font-mono-data text-dark-text">
                            ≈ {estimatedTotal.toFixed(2)} {quoteCurrency}
                        </span>
                    </div>
                )}

                {/* Submit Button */}
                <button
                    type="submit"
                    className={clsx(
                        "w-full py-3 rounded font-bold mt-auto transition-all",
                        side === 'BUY' ? "btn-buy" : "btn-sell"
                    )}
                >
                    {side} {baseCurrency}
                </button>
            </form>
        </div>
    );
};

export default OrderForm;
