import axios from 'axios';

// Base URL for the API Gateway
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

const client = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor to add JWT token and User ID header
client.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        const user = localStorage.getItem('user');

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

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
            // Clear local storage
            localStorage.removeItem('token');
            localStorage.removeItem('refreshToken');
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
