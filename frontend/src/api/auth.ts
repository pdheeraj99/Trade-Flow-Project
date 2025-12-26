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
    userId: string;
    username: string;
    email: string;
    roles: string[];
    accessTokenExpiresIn: number;
    refreshTokenExpiresIn: number;
    tokenType: string;
}

export interface LoginRequest {
    usernameOrEmail: string;
    password: string;
}

export interface RegisterRequest {
    username: string;
    email: string;
    password: string;
    firstName?: string;
    lastName?: string;
}

export interface RefreshTokenRequest {
    refreshToken: string;
}

export const authApi = {
    login: async (request: LoginRequest): Promise<AuthResponse> => {
        const response = await client.post<AuthResponse>('/auth/login', request);
        return response.data;
    },

    register: async (request: RegisterRequest): Promise<AuthResponse> => {
        const response = await client.post<AuthResponse>('/auth/register', request);
        return response.data;
    },

    refreshToken: async (request: RefreshTokenRequest): Promise<AuthResponse> => {
        const response = await client.post<AuthResponse>('/auth/refresh', request);
        return response.data;
    },

    logout: async (): Promise<void> => {
        await client.post('/auth/logout');
    }
};
