import axios from 'axios';
import useAuthStore from '../store/useAuthStore';

const api = axios.create({
    // 안드로이드 에뮬레이터에서 호스트(내 컴퓨터)의 스프링부트(8081)에 접속하려면 10.0.2.2를 써야 합니다. (8080은 도커 점유)
    baseURL: 'http://10.0.2.2:8081/api',
    timeout: 10000,
});

api.interceptors.request.use(
    async config => {
        console.log(`[네트워크 요청 🚀] ${config.method.toUpperCase()} ${config.baseURL}${config.url}`);
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
        console.error('[네트워크 요청 에러 💥]', error);
        return Promise.reject(error);
    }
);

api.interceptors.response.use(
    (response) => {
        console.log(`[네트워크 응답 ✅] ${response.status} from ${response.config.url}`);
        return response;
    },
    async (error) => {
        console.error(`[네트워크 응답 에러 🚨] ${error.response?.status} from ${error.config?.baseURL}${error.config?.url}`);
        console.error('상세 에러 내용:', error.response?.data || error.message);
        return Promise.reject(error);
    }
);

export default api;
