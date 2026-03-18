import axios from 'axios';
import useAuthStore from '../store/useAuthStore';

const api = axios.create({
    // 백엔드는 기본 포트 8080을 사용합니다.
    baseURL: 'http://10.0.2.2:8080/api',
    timeout: 10000,
});

api.interceptors.request.use(
    async config => {
        const fullUrl = `${config.baseURL.replace(/\/$/, '')}/${config.url.replace(/^\//, '')}`;
        console.log(`\n[네트워크 요청] ${config.method.toUpperCase()} ${fullUrl}`);
        if (config.data) console.log('[요청 데이터]:', JSON.stringify(config.data, null, 2));
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
        console.error('[네트워크 요청 에러]', error);
        return Promise.reject(error);
    }
);

api.interceptors.response.use(
    (response) => {
        const fullUrl = `${response.config.baseURL.replace(/\/$/, '')}/${response.config.url.replace(/^\//, '')}`;
        console.log(`[네트워크 응답] ${response.status} from ${fullUrl}`);
        if (response.data) console.log('[응답 데이터]:', JSON.stringify(response.data, null, 2));
        return response;
    },
    async (error) => {
        const fullUrl = error.config ? `${error.config.baseURL.replace(/\/$/, '')}/${error.config.url.replace(/^\//, '')}` : 'Unknown URL';
        console.error(`[네트워크 응답 에러] ${error.response?.status} from ${fullUrl}`);
        console.error('상세 에러 내용:', error.response?.data || error.message);
        return Promise.reject(error);
    }
);

export default api;
