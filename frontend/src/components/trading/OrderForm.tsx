import React, { useState } from 'react';
import clsx from 'clsx';

interface OrderFormProps {
    symbol: string;
    baseBalance: number;
    quoteBalance: number;
    onSubmit: (side: 'BUY' | 'SELL', type: 'LIMIT' | 'MARKET', price: number, quantity: number) => void;
}

const OrderForm: React.FC<OrderFormProps> = ({ symbol, baseBalance, quoteBalance, onSubmit }) => {
    const [side, setSide] = useState<'BUY' | 'SELL'>('BUY');
    const [type, setType] = useState<'LIMIT' | 'MARKET'>('LIMIT');
    const [price, setPrice] = useState('');
    const [quantity, setQuantity] = useState('');

    const baseCurrency = symbol.replace('USDT', '');
    const quoteCurrency = 'USDT';

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!quantity || (type === 'LIMIT' && !price)) return;

        onSubmit(side, type, Number(price), Number(quantity));
    };

    return (
        <div className="bg-dark-card border border-border rounded p-4 h-full">
            <div className="flex mb-4 gap-2">
                <button
                    className={clsx("flex-1 py-1.5 rounded font-bold text-sm transition-colors", {
                        'bg-trade-buy text-white': side === 'BUY',
                        'bg-secondary text-dark-muted hover:text-foreground': side !== 'BUY'
                    })}
                    onClick={() => setSide('BUY')}
                >
                    Buy
                </button>
                <button
                    className={clsx("flex-1 py-1.5 rounded font-bold text-sm transition-colors", {
                        'bg-trade-sell text-white': side === 'SELL',
                        'bg-secondary text-dark-muted hover:text-foreground': side !== 'SELL'
                    })}
                    onClick={() => setSide('SELL')}
                >
                    Sell
                </button>
            </div>

            <div className="flex mb-4 text-xs font-semibold text-dark-muted gap-4">
                <button
                    className={clsx("hover:text-primary", { 'text-primary': type === 'LIMIT' })}
                    onClick={() => setType('LIMIT')}
                >
                    Limit
                </button>
                <button
                    className={clsx("hover:text-primary", { 'text-primary': type === 'MARKET' })}
                    onClick={() => setType('MARKET')}
                >
                    Market
                </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-3">
                {type === 'LIMIT' && (
                    <div>
                        <label className="block text-xs text-dark-muted mb-1">Price ({quoteCurrency})</label>
                        <input
                            type="number"
                            value={price}
                            onChange={(e) => setPrice(e.target.value)}
                            className="w-full bg-dark-bg border border-border rounded p-2 text-right text-sm focus:border-primary outline-none text-foreground"
                            placeholder="0.00"
                            step="0.01"
                        />
                    </div>
                )}

                <div>
                    <label className="block text-xs text-dark-muted mb-1">Amount ({baseCurrency})</label>
                    <input
                        type="number"
                        value={quantity}
                        onChange={(e) => setQuantity(e.target.value)}
                        className="w-full bg-dark-bg border border-border rounded p-2 text-right text-sm focus:border-primary outline-none text-foreground"
                        placeholder="0.00"
                        step="0.0001"
                    />
                </div>

                {/* Percentage Slider Placeholder */}
                <div className="flex justify-between text-xs text-dark-muted mt-2">
                    <span>Avail: {(side === 'BUY' ? quoteBalance : baseBalance).toFixed(4)} {side === 'BUY' ? quoteCurrency : baseCurrency}</span>
                </div>

                <button
                    type="submit"
                    className={clsx("w-full py-2.5 rounded font-bold mt-4 text-white transition-opacity hover:opacity-90", {
                        'bg-trade-buy': side === 'BUY',
                        'bg-trade-sell': side === 'SELL'
                    })}
                >
                    {side} {baseCurrency}
                </button>
            </form>
        </div>
    );
};

export default OrderForm;
