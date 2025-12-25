import React from 'react';
import { walletApi } from '../../api/wallet';
import type { WalletBalance } from '../../api/wallet';
import { useAuth } from '../../context/AuthContext';

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
        <div className="bg-dark-card border border-border rounded h-full flex flex-col">
            <div className="p-3 border-b border-border flex justify-between items-center bg-dark-bg/50">
                <h3 className="font-bold text-sm text-foreground">Wallet</h3>
                <button
                    onClick={handleFaucet}
                    disabled={isLoading}
                    className="text-xs bg-primary/20 text-primary hover:bg-primary/30 px-2 py-1 rounded transition-colors disabled:opacity-50"
                >
                    {isLoading ? 'Claiming...' : 'Faucet'}
                </button>
            </div>
            <div className="overflow-auto flex-1">
                <table className="w-full text-xs text-left">
                    <thead className="text-dark-muted bg-dark-bg sticky top-0">
                        <tr>
                            <th className="p-2 font-medium">Asset</th>
                            <th className="p-2 font-medium text-right">Total</th>
                            <th className="p-2 font-medium text-right">Available</th>
                            <th className="p-2 font-medium text-right">Reserved</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-border/50">
                        {balances.length > 0 ? (
                            balances.map((balance) => (
                                <tr key={balance.currency} className="hover:bg-dark-bg/30 text-foreground">
                                    <td className="p-2 font-bold">{balance.currency}</td>
                                    <td className="p-2 text-right">{balance.total.toFixed(4)}</td>
                                    <td className="p-2 text-right">{balance.available.toFixed(4)}</td>
                                    <td className="p-2 text-right text-dark-muted">{balance.reserved.toFixed(4)}</td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={4} className="p-4 text-center text-dark-muted">No balances found</td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default Wallet;
