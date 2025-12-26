import client from './client';

export interface User {
    id: string;  // UUID from backend
    email: string;
    username: string;
    role: string;
}

export interface AuthResponse {
    accessToken: string;
    refreshToken: string;
    user: User;
}

export interface RegisterRequest {
    username: string;
    email: string;
    password: string;
    firstName?: string;
    lastName?: string;
}

export const authApi = {
    login: async (usernameOrEmail: string, password: string): Promise<AuthResponse> => {
        // Backend expects 'usernameOrEmail' field, not 'email'
        const response = await client.post<AuthResponse>('/auth/login', { usernameOrEmail, password });
        return response.data;
    },

    register: async (request: RegisterRequest): Promise<AuthResponse> => {
        // Backend expects username, email, password, firstName, lastName
        const response = await client.post<AuthResponse>('/auth/register', request);
        return response.data;
    },

    logout: async (): Promise<void> => {
        await client.post('/auth/logout');
    }
};
