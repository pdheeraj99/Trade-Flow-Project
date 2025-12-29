import axios from 'axios';

// Base URL for the API Gateway
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

const client = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
});

// Request interceptor to add User ID header (tokens are now in HttpOnly cookies)
client.interceptors.request.use(
    (config) => {
        const user = localStorage.getItem('user');

        // Add X-User-Id header for backend services (required by OMS, Wallet, etc.)
        if (user) {
            try {
                const userData = JSON.parse(user);
                if (userData.id) {
                    config.headers['X-User-Id'] = userData.id;
                }
            } catch (e) {
                console.error('Failed to parse user data from localStorage', e);
            }
        }

        return config;
    },
    (error) => Promise.reject(error)
);

// Response interceptor to handle errors (e.g., 401 Unauthorized)
client.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            // Clear user data (cookies will be cleared by backend on logout or expired)
            localStorage.removeItem('user');

            // Dispatch custom event to notify AuthContext
            window.dispatchEvent(new Event('auth:logout'));

            // Redirect to login
            if (!window.location.pathname.includes('/login')) {
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);

export default client;
