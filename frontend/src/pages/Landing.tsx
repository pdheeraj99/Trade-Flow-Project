import React from 'react';
import { Link } from 'react-router-dom';
import Navbar from '../components/layout/Navbar';
import styles from './Landing.module.css';

const Landing: React.FC = () => {
    return (
        <div className={styles.container}>
            <Navbar />

            <main className={styles.hero}>
                <h1 className={styles.title}>
                    TradeFlow
                </h1>
                <p className={styles.subtitle}>
                    High-Frequency Trading Simulation Platform. Zero Latency. Real Assets.
                    Experience the future of crypto trading with our professional-grade terminal.
                </p>

                <div className={styles.ctaGroup}>
                    <Link to="/login" className={styles.secondaryBtn}>
                        Start Trading
                    </Link>
                    <Link to="/register" className={styles.primaryBtn}>
                        Register Now
                    </Link>
                </div>
            </main>

            <section className={styles.features}>
                <div className={styles.featureCard}>
                    <h3 className={styles.featureTitle}>Real-time Orderbook</h3>
                    <p className={styles.featureDesc}>
                        Visualize market depth with our ultra-fast order matching engine.
                    </p>
                </div>
                <div className={styles.featureCard}>
                    <h3 className={styles.featureTitle}>Professional Charts</h3>
                    <p className={styles.featureDesc}>
                        Advanced technical analysis tools powered by Lightweight Charts.
                    </p>
                </div>
                <div className={styles.featureCard}>
                    <h3 className={styles.featureTitle}>Zero Latency</h3>
                    <p className={styles.featureDesc}>
                        Optimized for speed. Execute trades with millisecond precision.
                    </p>
                </div>
            </section>
        </div>
    );
};

export default Landing;
