import React from 'react';
import type { Order } from '../../api/orders';
import clsx from 'clsx';

interface ActiveOrdersProps {
    orders: Order[];
}

const ActiveOrders: React.FC<ActiveOrdersProps> = ({ orders }) => {
    return (
        <div className="bg-dark-card border border-border rounded h-full flex flex-col">
            <div className="p-3 border-b border-border bg-dark-bg/50">
                <h3 className="font-bold text-sm text-foreground">Active Orders</h3>
            </div>
            <div className="overflow-auto flex-1">
                <table className="w-full text-xs text-left">
                    <thead className="text-dark-muted bg-dark-bg sticky top-0">
                        <tr>
                            <th className="p-2 font-medium">Time</th>
                            <th className="p-2 font-medium">Symbol</th>
                            <th className="p-2 font-medium">Side</th>
                            <th className="p-2 font-medium text-right">Price</th>
                            <th className="p-2 font-medium text-right">Qty</th>
                            <th className="p-2 font-medium text-right">Filled</th>
                            <th className="p-2 font-medium text-center">Status</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-border/50 text-foreground">
                        {orders.length > 0 ? (
                            orders.map((order) => (
                                <tr key={order.orderId} className="hover:bg-dark-bg/30">
                                    <td className="p-2 text-dark-muted">{new Date(order.createdAt).toLocaleTimeString()}</td>
                                    <td className="p-2 font-bold">{order.symbol}</td>
                                    <td className={clsx("p-2 font-bold", {
                                        'text-trade-buy': order.side === 'BUY',
                                        'text-trade-sell': order.side === 'SELL'
                                    })}>
                                        {order.side}
                                    </td>
                                    <td className="p-2 text-right">{order.type === 'MARKET' ? 'Market' : order.price}</td>
                                    <td className="p-2 text-right">{order.quantity}</td>
                                    <td className="p-2 text-right">{order.filledQuantity}</td>
                                    <td className="p-2 text-center">
                                        <span className={clsx("px-1.5 py-0.5 rounded text-[10px]", {
                                            'bg-yellow-500/20 text-yellow-500': order.status === 'OPEN' || order.status === 'PARTIALLY_FILLED',
                                            'bg-green-500/20 text-green-500': order.status === 'FILLED',
                                            'bg-red-500/20 text-red-500': order.status === 'CANCELLED' || order.status === 'REJECTED'
                                        })}>
                                            {order.status}
                                        </span>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={7} className="p-4 text-center text-dark-muted">No active orders</td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default ActiveOrders;
