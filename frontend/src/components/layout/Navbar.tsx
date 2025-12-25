import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { LogOut, User, Wallet } from 'lucide-react';

const Navbar: React.FC = () => {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    return (
        <nav className="h-14 bg-dark-card border-b border-border flex items-center justify-between px-4">
            <div className="flex items-center gap-8">
                <Link to="/" className="text-xl font-bold text-primary flex items-center gap-2">
                    <div className="w-8 h-8 bg-primary rounded-full flex items-center justify-center text-black font-black">TF</div>
                    TradeFlow
                </Link>
                <div className="flex gap-6 text-sm font-medium text-dark-text">
                    <Link to="/dashboard" className="hover:text-primary transition-colors">Markets</Link>
                    <Link to="/dashboard" className="hover:text-primary transition-colors text-primary">Trade</Link>
                    <Link to="#" className="hover:text-primary transition-colors">Derivatives</Link>
                </div>
            </div>

            <div className="flex items-center gap-4">
                {user ? (
                    <>
                        <div className="flex items-center gap-2 text-sm text-dark-text hover:text-primary cursor-pointer">
                            <Wallet size={18} />
                            <span>Wallet</span>
                        </div>
                        <div className="flex items-center gap-2 text-sm text-dark-text">
                            <User size={18} />
                            <span>{user.email}</span>
                        </div>
                        <button
                            onClick={handleLogout}
                            className="text-dark-muted hover:text-white transition-colors"
                        >
                            <LogOut size={18} />
                        </button>
                    </>
                ) : (
                    <div className="flex gap-2">
                        <Link to="/login" className="px-4 py-1.5 text-sm font-medium hover:text-primary transition-colors">Log In</Link>
                        <Link to="/register" className="px-4 py-1.5 bg-primary text-black text-sm font-bold rounded hover:opacity-90 transition-opacity">Register</Link>
                    </div>
                )}
            </div>
        </nav>
    );
};

export default Navbar;
