import axios from 'axios';
import useAuthStore from '../store/useAuthStore';

const api = axios.create({
    baseURL: 'http://localhost:8080/api', // 더미 BASE URL
    timeout: 10000,
});

api.interceptors.request.use(
    async config => {
        const token = useAuthStore.getState().token;
        if (token) {
            config.headers = {
                ...config.headers,
                Authorization: `Bearer ${token}`,
            }
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

api.interceptors.response.use(
    (response) => {
        return response;
    },
    async (error) => {
        return Promise.reject(error);
    }
);

export default api;
