import React from 'react';

const Dashboard: React.FC = () => {
    return (
        <div className="min-h-screen bg-background text-foreground p-4">
            <header className="flex justify-between items-center mb-6">
                <h1 className="text-2xl font-bold text-primary">TradeFlow Terminal</h1>
                <div className="text-muted-foreground">BTC/USDT</div>
            </header>

            <div className="grid grid-cols-12 gap-4 h-[calc(100vh-100px)]">
                {/* Chart Area */}
                <div className="col-span-9 bg-card rounded p-4 border border-border">
                    Trading Chart Placeholder
                </div>

                {/* Order Book Area */}
                <div className="col-span-3 flex flex-col gap-4">
                    <div className="bg-card rounded p-4 border border-border flex-1">
                        Order Book Placeholder
                    </div>
                    <div className="bg-card rounded p-4 border border-border flex-1">
                        Order Form (Buy/Sell)
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;
