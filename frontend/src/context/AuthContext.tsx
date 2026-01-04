import React, { createContext, useContext, useState, useEffect } from 'react';
import type { User } from '../api/auth';
import client from '../api/client';
import { clearTokens, getAccessToken } from '../utils/tokens';

interface AuthContextType {
    user: User | null;
    isAuthenticated: boolean;
    login: (userData: User) => void;
    logout: () => void;
    loading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Check if user is already logged in by fetching profile
        const checkAuth = async () => {
            try {
                const token = getAccessToken();
                if (!token) {
                    setLoading(false);
                    return;
                }
                const response = await client.get<User>('/auth/me');
                setUser(response.data);
                localStorage.setItem('user', JSON.stringify(response.data));
            } catch (error) {
                console.debug('User not authenticated');
                setUser(null);
                localStorage.removeItem('user');
                clearTokens();
            } finally {
                setLoading(false);
            }
        };

        checkAuth();

        // Listen for logout events from API interceptor
        const handleLogoutEvent = () => {
            setUser(null);
            clearTokens();
            localStorage.removeItem('user');
        };

        window.addEventListener('auth:logout', handleLogoutEvent);

        return () => {
            window.removeEventListener('auth:logout', handleLogoutEvent);
        };
    }, []);

    const login = (userData: User) => {
        localStorage.setItem('user', JSON.stringify(userData));
        setUser(userData);
    };

    const logout = () => {
        localStorage.removeItem('user');
        clearTokens();
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, logout, loading }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
