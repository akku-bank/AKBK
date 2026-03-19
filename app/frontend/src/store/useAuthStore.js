import { create } from 'zustand';

const useAuthStore = create((set) => ({
    user: null, // { role: 'PARENT' | 'CHILD', name: string, ... }
    token: null,
    isAuthenticated: false,
    login: (user, token) => set({ user, token, isAuthenticated: true }),
    logout: () => set({ user: null, token: null, isAuthenticated: false }),
    setUser: (user) => set({ user }),
    setAuthInfo: (token, role, name) => set((state) => {
        const newUser = role ? { ...(state.user || {}), role, name } : state.user;
        return {
            token,
            user: newUser,
            isAuthenticated: !!newUser
        };
    }),
}));

export default useAuthStore;
