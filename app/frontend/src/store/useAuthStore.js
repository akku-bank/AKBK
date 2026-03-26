import { create } from 'zustand';

const useAuthStore = create((set) => ({
    user: null, // { role: 'PARENT' | 'CHILD', name: string, ... }
    token: null,
    isAuthenticated: false,
    login: (user, token) => set({ user, token, isAuthenticated: true }),
    logout: () => set({ user: null, token: null, isAuthenticated: false, cachedLevel: null }),
    setUser: (user) => set({ user }),
    setAuthInfo: (token, role, name, id, extra = {}) => set((state) => {
        const newId = id !== undefined ? id : state.user?.id;
        const newUser = role ? {
            ...(state.user || {}),
            role,
            name,
            id: newId,
            ...extra
        } : state.user;
        return {
            token,
            user: newUser,
            isAuthenticated: !!newUser
        };
    }),
    cachedLevel: null,
    setCachedLevel: (level) => set({ cachedLevel: level }),
}));

export default useAuthStore;
