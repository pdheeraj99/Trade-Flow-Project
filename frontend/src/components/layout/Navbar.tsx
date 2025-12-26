import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { LogOut, User, Wallet, Activity } from 'lucide-react';
import clsx from 'clsx';
import styles from './Navbar.module.css';

interface NavbarProps {
    isConnected?: boolean;
    connectionError?: string | null;
}

const Navbar: React.FC<NavbarProps> = ({ isConnected = false, connectionError = null }) => {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    // Determine connection status
    const connectionStatus = isConnected ? 'connected' : connectionError ? 'disconnected' : 'connecting';
    const statusTooltip = isConnected ? 'Connected' : connectionError || 'Connecting...';

    return (
        <nav className={styles.navbar}>
            <div className={styles.leftSection}>
                {/* Logo */}
                <Link to="/" className={styles.logo}>
                    <div className={styles.logoIcon}>
                        TF
                    </div>
                    <span className={styles.logoText}>TradeFlow</span>
                </Link>

                {/* Navigation Links */}
                <div className={styles.navLinks}>
                    <Link to="/dashboard" className={styles.navLink}>
                        Markets
                    </Link>
                    <Link to="/dashboard" className={clsx(styles.navLink, styles.navLinkActive)}>
                        <Activity size={14} />
                        Trade
                    </Link>
                    <Link to="#" className={styles.navLink}>
                        Derivatives
                    </Link>
                </div>
            </div>

            <div className={styles.rightSection}>
                {/* Connection Status Indicator */}
                <div className={styles.statusContainer} title={statusTooltip}>
                    <div className={clsx(styles.statusIndicator, styles[connectionStatus])} />
                    <span className={styles.statusText}>
                        {isConnected ? 'Live' : 'Offline'}
                    </span>
                </div>

                {user ? (
                    <>
                        {/* Wallet Link */}
                        <div className={styles.walletLink}>
                            <Wallet size={16} />
                            <span className={styles.walletText}>Wallet</span>
                        </div>

                        {/* User Info */}
                        <div className={styles.userInfo}>
                            <User size={16} className={styles.userInfoIcon} />
                            <span className="max-w-[120px] truncate">{user.email}</span>
                        </div>

                        {/* Logout */}
                        <button
                            onClick={handleLogout}
                            className={styles.logoutBtn}
                            title="Logout"
                        >
                            <LogOut size={18} />
                        </button>
                    </>
                ) : (
                    <div className={styles.authButtons}>
                        <Link
                            to="/login"
                            className={styles.loginBtn}
                        >
                            Log In
                        </Link>
                        <Link
                            to="/register"
                            className={styles.registerBtn}
                        >
                            Register
                        </Link>
                    </div>
                )}
            </div>
        </nav>
    );
};

export default Navbar;
