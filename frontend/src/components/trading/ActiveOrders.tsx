import React from 'react';
import type { Order } from '../../api/orders';
import clsx from 'clsx';
import { X } from 'lucide-react';
import styles from './ActiveOrders.module.css';

interface ActiveOrdersProps {
    orders: Order[];
    onCancelOrder?: (orderId: string) => void;
}

const ActiveOrders: React.FC<ActiveOrdersProps> = ({ orders, onCancelOrder }) => {
    const handleCancel = (orderId: string) => {
        if (onCancelOrder) {
            onCancelOrder(orderId);
        } else {
            console.log('Cancel order:', orderId);
        }
    };

    return (
        <div className={styles.container}>
            <div className={styles.tableContainer}>
                <table className={styles.table}>
                    <thead className={styles.thead}>
                        <tr>
                            <th className={styles.th}>Time</th>
                            <th className={styles.th}>Symbol</th>
                            <th className={styles.th}>Side</th>
                            <th className={clsx(styles.th, styles.thRight)}>Price</th>
                            <th className={clsx(styles.th, styles.thRight)}>Qty</th>
                            <th className={clsx(styles.th, styles.thRight)}>Filled</th>
                            <th className={clsx(styles.th, styles.thCenter)}>Status</th>
                            <th className={clsx(styles.th, styles.thCenter)} style={{ width: '40px' }}></th>
                        </tr>
                    </thead>
                    <tbody>
                        {orders.length > 0 ? (
                            orders.map((order) => (
                                <tr
                                    key={order.orderId}
                                    className={styles.row}
                                >
                                    <td className={styles.cellTime}>
                                        {new Date(order.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                                    </td>
                                    <td className={styles.cellSymbol}>{order.symbol}</td>
                                    <td className={clsx(styles.cellSide, {
                                        [styles.cellSideBuy]: order.side === 'BUY',
                                        [styles.cellSideSell]: order.side === 'SELL'
                                    })}>
                                        {order.side}
                                    </td>
                                    <td className={styles.cellMono}>
                                        {order.type === 'MARKET' ? 'Market' : order.price?.toFixed(2)}
                                    </td>
                                    <td className={styles.cellMono}>{order.quantity.toFixed(5)}</td>
                                    <td className={styles.cellMonoMuted}>
                                        {order.filledQuantity?.toFixed(5) || '0.00000'}
                                    </td>
                                    <td className={styles.cellCenter}>
                                        <span className={clsx(styles.statusBadge, {
                                            [styles.statusOpen]: order.status === 'OPEN' || order.status === 'PARTIALLY_FILLED',
                                            [styles.statusFilled]: order.status === 'FILLED',
                                            [styles.statusCancelled]: order.status === 'CANCELLED' || order.status === 'REJECTED'
                                        })}>
                                            {order.status}
                                        </span>
                                    </td>
                                    <td className={styles.cellCenter}>
                                        {(order.status === 'OPEN' || order.status === 'PARTIALLY_FILLED') && (
                                            <button
                                                onClick={() => handleCancel(order.orderId)}
                                                className={styles.cancelBtn}
                                                title="Cancel Order"
                                            >
                                                <X size={14} />
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={8} className={styles.emptyState}>
                                    <div className={styles.emptyContent}>
                                        <div className={styles.emptyIcon}>📋</div>
                                        <span>No active orders</span>
                                    </div>
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default ActiveOrders;
