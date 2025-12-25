import React, { useEffect, useRef } from 'react';
import { createChart, ColorType } from 'lightweight-charts';
import type { IChartApi, ISeriesApi, CandlestickData } from 'lightweight-charts';

interface TradingChartProps {
    data: CandlestickData[];
    symbol: string;
}

const TradingChart: React.FC<TradingChartProps> = ({ data, symbol }) => {
    const chartContainerRef = useRef<HTMLDivElement>(null);
    const chartRef = useRef<IChartApi | null>(null);
    const seriesRef = useRef<ISeriesApi<"Candlestick"> | null>(null);

    // Use symbol to trigger re-render or just suppress warning
    useEffect(() => {
        // console.log("Chart symbol:", symbol); 
    }, [symbol]);

    useEffect(() => {
        if (!chartContainerRef.current) return;

        const chart = createChart(chartContainerRef.current, {
            layout: {
                background: { type: ColorType.Solid, color: '#181A20' }, // Card BG
                textColor: '#EAECEF',
            },
            grid: {
                vertLines: { color: '#2B3139' },
                horzLines: { color: '#2B3139' },
            },
            width: chartContainerRef.current.clientWidth,
            height: 500,
            timeScale: {
                timeVisible: true,
                secondsVisible: false,
            },
        });

        const candlestickSeries = (chart as any).addCandlestickSeries({
            upColor: '#00c076',       // Binance Green
            downColor: '#ff5353',     // Binance Red
            borderUpColor: '#00c076',
            borderDownColor: '#ff5353',
            wickUpColor: '#00c076',
            wickDownColor: '#ff5353',
        });

        candlestickSeries.setData(data);

        chartRef.current = chart;
        seriesRef.current = candlestickSeries;

        const handleResize = () => {
            if (chartContainerRef.current) {
                chart.applyOptions({ width: chartContainerRef.current.clientWidth });
            }
        };

        window.addEventListener('resize', handleResize);

        return () => {
            window.removeEventListener('resize', handleResize);
            chart.remove();
        };
    }, []); // Init chart only once

    // Update data when props change
    useEffect(() => {
        if (seriesRef.current && data.length > 0) {
            seriesRef.current.setData(data); // In real app, we'd use .update() for incremental data
        }
    }, [data]);

    return <div ref={chartContainerRef} className="w-full h-[500px]" />;
};

export default TradingChart;
