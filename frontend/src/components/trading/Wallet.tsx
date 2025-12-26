import React from 'react';
import { walletApi } from '../../api/wallet';
import type { WalletBalance } from '../../api/wallet';
import { useAuth } from '../../context/AuthContext';
import clsx from 'clsx';
import styles from './Wallet.module.css';

interface WalletProps {
    balances: WalletBalance[];
    onRefresh: () => void;
}

const Wallet: React.FC<WalletProps> = ({ balances, onRefresh }) => {
    const { user } = useAuth();
    const [isLoading, setIsLoading] = React.useState(false);

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
