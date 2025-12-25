import client from './client';

export interface User {
    id: number;
    email: string;
    role: string;
}

export interface AuthResponse {
    accessToken: string;
    refreshToken: string;
    user: User;
}

export const authApi = {
    login: async (email: string, password: string): Promise<AuthResponse> => {
        const response = await client.post<AuthResponse>('/auth/login', { email, password });
        return response.data;
    },

    register: async (email: string, password: string, fullName: string): Promise<User> => {
        const response = await client.post<User>('/auth/register', { email, password, fullName });
        return response.data;
    },

    logout: async (): Promise<void> => {
        await client.post('/auth/logout');
    }
};
