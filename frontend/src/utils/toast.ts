import toast from 'react-hot-toast';

/**
 * Centralized toast notification service
 * Provides consistent user feedback across the application
 */
export const toastService = {
    /**
     * Success notification
     */
    success: (message: string) => {
        toast.success(message, {
            duration: 3000,
            position: 'top-right',
            style: {
                background: '#10b981',
                color: '#fff',
                fontWeight: '600',
            },
        });
    },

    /**
     * Error notification
     */
    error: (message: string) => {
        toast.error(message, {
            duration: 4000,
            position: 'top-right',
            style: {
                background: '#ef4444',
                color: '#fff',
                fontWeight: '600',
            },
        });
    },

    /**
     * Info notification
     */
    info: (message: string) => {
        toast(message, {
            duration: 3000,
            position: 'top-right',
            icon: 'ℹ️',
            style: {
                background: '#3b82f6',
                color: '#fff',
                fontWeight: '600',
            },
        });
    },

    /**
     * Warning notification
     */
    warning: (message: string) => {
        toast(message, {
            duration: 3500,
            position: 'top-right',
            icon: '⚠️',
            style: {
                background: '#f59e0b',
                color: '#fff',
                fontWeight: '600',
            },
        });
    },

    /**
     * Loading notification (returns toast ID for dismissal)
     */
    loading: (message: string) => {
        return toast.loading(message, {
            position: 'top-right',
        });
    },

    /**
     * Dismiss a specific toast by ID
     */
    dismiss: (toastId: string) => {
        toast.dismiss(toastId);
    },

    /**
     * Order-specific notifications
     */
    order: {
        created: (symbol: string, side: string) => {
            toast.success(`${side} order placed for ${symbol}`, {
                duration: 3000,
                position: 'top-right',
                icon: '✓',
            });
        },

        cancelled: (orderId: string) => {
            toast.success('Order cancelled successfully', {
                duration: 3000,
                position: 'top-right',
            });
        },

        filled: (symbol: string, quantity: number) => {
            toast.success(`Order filled: ${quantity} ${symbol}`, {
                duration: 4000,
                position: 'top-right',
                icon: '🎯',
            });
        },

        partiallyFilled: (symbol: string, filled: number, total: number) => {
            toast.info(`Partially filled: ${filled}/${total} ${symbol}`, {
                duration: 3500,
                position: 'top-right',
            });
        },

        failed: (error: string) => {
            toast.error(error || 'Order placement failed', {
                duration: 4000,
                position: 'top-right',
            });
        }
    },

    /**
     * Wallet-specific notifications
     */
    wallet: {
        faucetSuccess: (amount: number, currency: string) => {
            toast.success(`Faucet claimed: ${amount} ${currency}`, {
                duration: 3000,
                position: 'top-right',
                icon: '💰',
            });
        },

        insufficientBalance: () => {
            toast.error('Insufficient balance', {
                duration: 3000,
                position: 'top-right',
            });
        }
    },

    /**
     * Auth-specific notifications
     */
    auth: {
        loginSuccess: (username: string) => {
            toast.success(`Welcome back, ${username}!`, {
                duration: 2500,
                position: 'top-right',
            });
        },

        registrationSuccess: () => {
            toast.success('Account created successfully!', {
                duration: 3000,
                position: 'top-right',
            });
        },

        logoutSuccess: () => {
            toast.success('Logged out successfully', {
                duration: 2000,
                position: 'top-right',
            });
        },

        sessionExpired: () => {
            toast.error('Session expired. Please log in again.', {
                duration: 4000,
                position: 'top-right',
            });
        }
    }
};
