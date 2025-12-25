import React from 'react';
import { Link } from 'react-router-dom';

const Register: React.FC = () => {
    return (
        <div className="min-h-screen bg-background flex items-center justify-center p-4">
            <div className="bg-card p-8 rounded-lg border border-border w-full max-w-md">
                <h2 className="text-3xl font-bold text-primary mb-6 text-center">Create Account</h2>

                <form className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-muted-foreground mb-1">Full Name</label>
                        <input
                            type="text"
                            className="w-full bg-input border border-border rounded p-2 text-foreground focus:ring-2 focus:ring-primary focus:outline-none"
                            placeholder="John Doe"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-muted-foreground mb-1">Email</label>
                        <input
                            type="email"
                            className="w-full bg-input border border-border rounded p-2 text-foreground focus:ring-2 focus:ring-primary focus:outline-none"
                            placeholder="user@example.com"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-muted-foreground mb-1">Password</label>
                        <input
                            type="password"
                            className="w-full bg-input border border-border rounded p-2 text-foreground focus:ring-2 focus:ring-primary focus:outline-none"
                            placeholder="••••••••"
                        />
                    </div>

                    <button
                        type="submit"
                        className="w-full bg-primary text-primary-foreground font-bold py-2 rounded hover:opacity-90 transition-opacity"
                    >
                        Register
                    </button>
                </form>

                <p className="mt-4 text-center text-muted-foreground">
                    Already have an account? <Link to="/login" className="text-primary hover:underline">Login</Link>
                </p>
            </div>
        </div>
    );
};

export default Register;
