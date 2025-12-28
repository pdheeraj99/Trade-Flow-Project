import React, { useEffect, useState } from 'react';
import { walletApi } from '../../api/wallet';
import type { WalletBalance } from '../../api/wallet';
import { useAuth } from '../../context/AuthContext';
import clsx from 'clsx';
import styles from './Wallet.module.css';
import { useStompClient } from 'react-stomp-hooks';

interface WalletProps {
    onRefresh: () => void;
}

const Wallet: React.FC<WalletProps> = ({ onRefresh }) => {
    const { user } = useAuth();
    const userId = user?.id.toString();
    const [balances, setBalances] = useState<WalletBalance[]>([]);
    const [isLoading, setIsLoading] = React.useState(false);
    const stompClient = useStompClient();

    useEffect(() => {
        if (!stompClient || !userId) return;
        
        const subscription = stompClient.subscribe(
            `/topic/balances/${userId}`,
            (message) => {
                const updatedBalances = JSON.parse(message.body);
                setBalances(updatedBalances);
            }
        );
        
        return () => subscription.unsubscribe();
    }, [stompClient, userId]);

    useEffect(() => {
        if (!userId) return;
        walletApi.getBalances(userId).then((data) => setBalances(data));
    }, [userId]);

    const handleFaucet = async () => {
        if (!user) return;
        setIsLoading(true);
        try {
            await walletApi.faucet(user.id.toString());
            onRefresh();
        } catch (error) {
            console.error('Faucet failed', error);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <h3 className={styles.title}>Wallet</h3>
                <button
                    onClick={handleFaucet}
                    disabled={isLoading}
                    className={styles.faucetBtn}
                >
                    {isLoading ? 'Claiming...' : 'Faucet'}
                </button>
            </div>
            <div className={styles.tableContainer}>
                <table className={styles.table}>
                    <thead className={styles.thead}>
                        <tr>
                            <th className={styles.th}>Asset</th>
                            <th className={clsx(styles.th, styles.thRight)}>Total</th>
                            <th className={clsx(styles.th, styles.thRight)}>Available</th>
                            <th className={clsx(styles.th, styles.thRight)}>Reserved</th>
                        </tr>
                    </thead>
                    <tbody>
                        {balances.length > 0 ? (
                            balances.map((balance) => (
                                <tr key={balance.currency} className={styles.row}>
                                    <td className={styles.cellAsset}>{balance.currency}</td>
                                    <td className={styles.cellNumber}>{balance.total.toFixed(4)}</td>
                                    <td className={styles.cellNumber}>{balance.available.toFixed(4)}</td>
                                    <td className={styles.cellReserved}>{balance.reserved.toFixed(4)}</td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={4} className={styles.emptyState}>No balances found</td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default Wallet;
