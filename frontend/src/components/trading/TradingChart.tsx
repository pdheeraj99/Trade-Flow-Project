import React, { useEffect, useRef, useCallback } from 'react';
import { createChart, ColorType, CandlestickSeries } from 'lightweight-charts';
import type { IChartApi, ISeriesApi, CandlestickData, Time } from 'lightweight-charts';
import styles from './TradingChart.module.css';

interface TradingChartProps {
    data: CandlestickData[];
    symbol: string;
    onCandleUpdate?: (candle: CandlestickData) => void;
}

/**
 * Helper function to normalize time to a timestamp number
 * Handles both string (YYYY-MM-DD) and number formats
 */
const getTimestamp = (time: Time): number => {
    if (typeof time === 'string') {
        return new Date(time).getTime();
    }
    return Number(time);
};

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
                background: { type: ColorType.Solid, color: '#1E2329' }, // var(--bg-surface)
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
                    color: '#FCD535', // var(--text-accent)
                    labelBackgroundColor: '#FCD535',
                },
                horzLine: {
                    color: '#FCD535',
                    labelBackgroundColor: '#FCD535',
                },
            },
        });

        const candlestickSeries = chart.addSeries(CandlestickSeries, {
            upColor: '#0ecb81', // var(--trade-buy)
            downColor: '#f6465d', // var(--trade-sell)
            borderUpColor: '#0ecb81',
            borderDownColor: '#f6465d',
            wickUpColor: '#0ecb81',
            wickDownColor: '#f6465d',
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

        // CRITICAL: Deep clone data to prevent lightweight-charts from mutating our parent state
        // The library converts time strings to timestamps on the original object references
        const clonedData = newData.map(candle => ({
            time: candle.time,
            open: candle.open,
            high: candle.high,
            low: candle.low,
            close: candle.close,
        }));

        // Deduplicate using Map keyed by normalized timestamp
        // This ensures we only have one candle per unique time point
        const deduped = new Map<number, CandlestickData>();
        for (const candle of clonedData) {
            const timestamp = getTimestamp(candle.time);
            deduped.set(timestamp, candle);
        }

        // Sort by timestamp in ascending order (required by lightweight-charts)
        const sortedData = Array.from(deduped.values()).sort((a, b) => {
            return getTimestamp(a.time) - getTimestamp(b.time);
        });

        if (sortedData.length === 0) return;

        // If we have a last candle, check if we should update or add
        if (lastCandleTimeRef.current) {
            const lastNewCandle = sortedData[sortedData.length - 1];
            const lastNewTimestamp = getTimestamp(lastNewCandle.time);
            const lastTrackedTimestamp = getTimestamp(lastCandleTimeRef.current);

            // Use update() for real-time updates to the last candle
            if (lastNewTimestamp === lastTrackedTimestamp) {
                seriesRef.current.update(lastNewCandle);
            } else {
                // New candle or data refresh, set all data
                seriesRef.current.setData(sortedData);
            }
        } else {
            // Initial data load
            seriesRef.current.setData(sortedData);
        }

        // Track the last candle time
        lastCandleTimeRef.current = sortedData[sortedData.length - 1].time;
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
