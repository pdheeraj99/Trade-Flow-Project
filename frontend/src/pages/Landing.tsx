import React from 'react';
import { Link } from 'react-router-dom';

const Landing: React.FC = () => {
    return (
        <div className="min-h-screen bg-background text-foreground flex flex-col items-center justify-center p-4">
            <h1 className="text-6xl font-bold text-primary mb-6">TradeFlow</h1>
            <p className="text-xl text-muted-foreground mb-8 text-center max-w-2xl">
                High-Frequency Trading Simulation Platform. Zero Latency. Real Assets.
            </p>
            <div className="flex gap-4">
                <Link
                    to="/login"
                    className="px-6 py-3 bg-primary text-primary-foreground font-semibold rounded hover:opacity-90 transition-opacity"
                >
                    Start Trading
                </Link>
                <Link
                    to="/register"
                    className="px-6 py-3 border border-border text-foreground font-semibold rounded hover:bg-secondary transition-colors"
                >
                    Register
                </Link>
            </div>
        </div>
    );
};

export default Landing;
