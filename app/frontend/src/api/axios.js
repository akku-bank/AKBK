import axios from 'axios';
import useAuthStore from '../store/useAuthStore';

const api = axios.create({
    baseURL: `${process.env.REACT_APP_API_URL}/api`,
    timeout: 10000,
});

api.interceptors.request.use(
    async config => {
        const fullUrl = `${config.baseURL.replace(/\/$/, '')}/${config.url.replace(/^\//, '')}`;
        console.log(`\n[네트워크 요청] ${config.method.toUpperCase()} ${fullUrl}`);
        if (config.data) console.log('[요청 데이터]:', JSON.stringify(config.data, null, 2));
        const token = useAuthStore.getState().token;
        // 로그인/회원가입 등 인증이 필요 없거나 토큰 자체가 필요한 라우트에는 헤더 추가 생략
        const isAuthRoute = config.url.includes('auth/social') || config.url.includes('auth/signup') || config.url.includes('auth/refresh');

        if (token && !isAuthRoute && !config.headers.Authorization) {
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
