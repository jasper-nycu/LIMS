import axios from 'axios';

/**
 * 1. Create a centralized Axios instance for the LIMS frontend.
 * We point the baseURL directly to our newly secured /api/v1 backend routes.
 */
const api = axios.create({
    baseURL: 'http://localhost:8080/api/v1',
    timeout: 10000, // 10 seconds timeout
    headers: {
        'Content-Type': 'application/json',
    }
});

/**
 * 2. Request Interceptor (The Gateway Out)
 * This runs BEFORE every request leaves the frontend.
 * It fetches the JWT from localStorage and attaches it to the Authorization header.
 */
api.interceptors.request.use(
    (config) => {
        // Retrieve the token from localStorage
        const token = localStorage.getItem('lims_jwt');
        
        // If a token exists, attach it to the header as a Bearer token
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        
        return config;
    },
    (error) => {
        // Handle request setup errors
        return Promise.reject(error);
    }
);

/**
 * 3. Response Interceptor (The Gateway In)
 * This runs when the backend responds, BEFORE the .then() or .catch() in your UI components.
 * It acts as a global error handler for authentication issues.
 */
api.interceptors.response.use(
    (response) => {
        // If the response is successful (2xx), just pass it through
        return response;
    },
    (error) => {
        // Only treat 401 as session expiry for routes that are NOT password verification.
        // Password-related endpoints return 401/422 for wrong-password errors, not token issues.
        const url = error.config?.url || '';
        const isPasswordRoute = url.includes('/me/password');

        if (!isPasswordRoute && error.response && (error.response.status === 401 || error.response.status === 403)) {
            console.warn('Authentication failed or token expired. Redirecting to login...');
            localStorage.removeItem('lims_jwt');
            window.dispatchEvent(new CustomEvent('auth-expired'));
        }
        
        return Promise.reject(error);
    }
);

export default api;