import React, { useEffect, useRef, useCallback } from 'react';
import { createChart, ColorType } from 'lightweight-charts';
import type { IChartApi, ISeriesApi, CandlestickData, Time } from 'lightweight-charts';
import styles from './TradingChart.module.css';

interface TradingChartProps {
    data: CandlestickData[];
    symbol: string;
    onCandleUpdate?: (candle: CandlestickData) => void;
}

const TradingChart: React.FC<TradingChartProps> = ({ data, symbol }) => {
    const chartContainerRef = useRef<HTMLDivElement>(null);
    const chartRef = useRef<IChartApi | null>(null);
    const seriesRef = useRef<ISeriesApi<"Candlestick"> | null>(null);
    const lastCandleTimeRef = useRef<Time | null>(null);

    // Initialize chart
    useEffect(() => {
        if (!chartContainerRef.current) return;

        const chart = createChart(chartContainerRef.current, {
            layout: {
                background: { type: ColorType.Solid, color: '#181A20' }, // var(--bg-card)
                textColor: '#EAECEF', // var(--text-primary)
            },
            grid: {
                vertLines: { color: '#2B3139' }, // var(--border-color)
                horzLines: { color: '#2B3139' },
            },
            width: chartContainerRef.current.clientWidth,
            height: chartContainerRef.current.clientHeight || 400,
            timeScale: {
                timeVisible: true,
                secondsVisible: false,
                borderColor: '#2B3139',
            },
            rightPriceScale: {
                borderColor: '#2B3139',
            },
            crosshair: {
                mode: 0, // Normal mode
                vertLine: {
                    color: '#FCD535', // var(--text-accent) - Updated from F0B90B
                    labelBackgroundColor: '#FCD535',
                },
                horzLine: {
                    color: '#FCD535',
                    labelBackgroundColor: '#FCD535',
                },
            },
        });

        const candlestickSeries = (chart as any).addCandlestickSeries({
            upColor: '#0ECB81', // var(--trade-buy) - Updated
            downColor: '#F6465D', // var(--trade-sell) - Updated
            borderUpColor: '#0ECB81',
            borderDownColor: '#F6465D',
            wickUpColor: '#0ECB81',
            wickDownColor: '#F6465D',
        });

        chartRef.current = chart;
        seriesRef.current = candlestickSeries;

        // Handle resize
        const handleResize = () => {
            if (chartContainerRef.current && chartRef.current) {
                chartRef.current.applyOptions({
                    width: chartContainerRef.current.clientWidth,
                    height: chartContainerRef.current.clientHeight,
                });
            }
        };

        const resizeObserver = new ResizeObserver(handleResize);
        resizeObserver.observe(chartContainerRef.current);

        window.addEventListener('resize', handleResize);

        return () => {
            window.removeEventListener('resize', handleResize);
            resizeObserver.disconnect();
            chart.remove();
            chartRef.current = null;
            seriesRef.current = null;
        };
    }, []);

    // Update chart with new data
    const updateChart = useCallback((newData: CandlestickData[]) => {
        if (!seriesRef.current || newData.length === 0) return;

        // If we have a last candle, check if we should update or add
        if (lastCandleTimeRef.current && newData.length > 0) {
            const lastNewCandle = newData[newData.length - 1];

            // Use update() for real-time updates to the last candle
            if (lastNewCandle.time === lastCandleTimeRef.current) {
                seriesRef.current.update(lastNewCandle);
            } else {
                // New candle, set all data
                seriesRef.current.setData(newData);
            }
        } else {
            // Initial data load
            seriesRef.current.setData(newData);
        }

        // Track the last candle time
        if (newData.length > 0) {
            lastCandleTimeRef.current = newData[newData.length - 1].time;
        }
    }, []);

    // React to data changes
    useEffect(() => {
        updateChart(data);
    }, [data, updateChart]);

    // React to symbol changes - reinitialize
    useEffect(() => {
        lastCandleTimeRef.current = null;
        if (seriesRef.current && data.length > 0) {
            seriesRef.current.setData(data);
            if (data.length > 0) {
                lastCandleTimeRef.current = data[data.length - 1].time;
            }
        }
    }, [symbol, data]);

    return (
        <div
            ref={chartContainerRef}
            className={styles.container}
        />
    );
};

export default TradingChart;
