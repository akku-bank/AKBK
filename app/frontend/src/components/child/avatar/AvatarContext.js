import React, { createContext, useState, useEffect } from 'react';
import api from '../../../api/axios';
import { AVATAR_ITEMS } from './AvatarAssets';
import useAuthStore from '../../../store/useAuthStore';

export const AvatarContext = createContext();

export const AvatarProvider = ({ children }) => {
    // 기본 상태
    const [equipState, setEquipState] = useState({
        gender: 'boy',
        face: 'base_boy',
        hair: 'hair_boy',
        upper: 'upper_base',
        lower: 'lower_base',
        hat: 'none',
        shoe: 'none',
        wing: 'none',
    });

    const updateEquip = (category, itemId) => {
        setEquipState(prev => ({
            ...prev,
            [category]: itemId
        }));
    };

    const { token, user } = useAuthStore();

    useEffect(() => {
        // 로그인 토큰이 없거나 자녀('CHILD')가 아니면 아바타 로드를 중단합니다.
        if (!token || user?.role !== 'CHILD') return;

        const fetchEquipped = async () => {
            try {
                const res = await api.get('/avatars/items');
                const items = res.data?.data?.items || [];

                let newEquip = {};
                let hasChanges = false;

                items.filter(i => i.isEquipped).forEach(backendItem => {
                    let frontendCat = null;
                    if (backendItem.category === 'HAT') frontendCat = 'hat';
                    else if (backendItem.category === 'TOP') frontendCat = 'upper';
                    else if (backendItem.category === 'BOTTOM') frontendCat = 'lower';

                    if (frontendCat && AVATAR_ITEMS[frontendCat]) {
                        const matching = AVATAR_ITEMS[frontendCat].find(i => i.name === backendItem.name);
                        if (matching) {
                            newEquip[frontendCat] = matching.id;
                            hasChanges = true;
                        }
                    }
                });

                if (hasChanges) {
                    setEquipState(prev => ({ ...prev, ...newEquip }));
                }
            } catch (e) {
                console.error('Avatar Initial Load Error', e);
            }
        };
        fetchEquipped();
    }, [token, user?.role]);

    return (
        <AvatarContext.Provider value={{ equipState, updateEquip, setEquipState }}>
            {children}
        </AvatarContext.Provider>
    );
};
