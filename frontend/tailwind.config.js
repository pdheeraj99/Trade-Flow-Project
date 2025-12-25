/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            colors: {
                background: "var(--background)",
                foreground: "var(--foreground)",
                border: "var(--border)",
                input: "var(--input)",
                ring: "var(--ring)",
                card: {
                    DEFAULT: "var(--card)",
                    foreground: "var(--card-foreground)",
                },
                trade: {
                    buy: "#00c076",         // Binance Green
                    sell: "#ff5353",        // Binance Red
                    buyBg: "rgba(0, 192, 118, 0.1)",
                    sellBg: "rgba(255, 83, 83, 0.1)",
                },
                dark: {
                    bg: "#0B0E11",          // Binance Dark BG
                    card: "#181A20",        // Binance Card BG
                    hover: "#2B3139",       // Binance Hover
                    text: "#EAECEF",        // Binance Text
                    muted: "#848E9C",       // Binance Muted
                }
            },
        },
    },
    plugins: [],
}
