/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            fontFamily: {
                mono: ['"JetBrains Mono"', '"Roboto Mono"', 'ui-monospace', 'SFMono-Regular', 'monospace'],
                sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
            },
            colors: {
                background: "var(--background)",
                foreground: "var(--foreground)",
                border: "var(--border)",
                input: "var(--input)",
                ring: "var(--ring)",
                primary: "var(--primary)",
                card: {
                    DEFAULT: "var(--card)",
                    foreground: "var(--card-foreground)",
                },
                trade: {
                    buy: "#0ecb81",         // Vibrant Neon Green
                    sell: "#f6465d",        // Vibrant Neon Red
                    buyBg: "rgba(14, 203, 129, 0.15)",
                    sellBg: "rgba(246, 70, 93, 0.15)",
                    buyGlow: "rgba(14, 203, 129, 0.4)",
                    sellGlow: "rgba(246, 70, 93, 0.4)",
                },
                dark: {
                    bg: "#0b0e11",          // Deep Void Black
                    card: "#181a20",        // Rich Charcoal
                    hover: "#2b3139",       // Hover State
                    text: "#eaecef",        // High-emphasis White
                    muted: "#848e9c",       // Muted Gray
                    border: "#2b3139",      // Border Color
                },
                accent: {
                    yellow: "#FCD535",      // Binance Yellow (Primary Action)
                    blue: "#1E90FF",        // Info Blue
                }
            },
            animation: {
                'flash-up': 'flashUp 500ms ease-out',
                'flash-down': 'flashDown 500ms ease-out',
                'pulse-slow': 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
            },
            keyframes: {
                flashUp: {
                    '0%': { backgroundColor: 'rgba(14, 203, 129, 0.4)' },
                    '100%': { backgroundColor: 'transparent' },
                },
                flashDown: {
                    '0%': { backgroundColor: 'rgba(246, 70, 93, 0.4)' },
                    '100%': { backgroundColor: 'transparent' },
                },
            },
            backdropBlur: {
                xs: '2px',
            },
        },
    },
    plugins: [],
}
