import React, { createContext, useState, useEffect } from 'react';
import api from '../../../api/axios';
import { AVATAR_ITEMS } from './AvatarAssets';
import useAuthStore from '../../../store/useAuthStore';

export const AvatarContext = createContext();

export const AvatarProvider = ({ children }) => {
    // 기본 상태 (AVATAR_ASSETS 딕셔너리 이름과 100% 동일하게 맞춰주세요!)
    const [equipState, setEquipState] = useState({
        gender: 'boy',
        face: 'boy1',
        hair: 'boy1',
        outfit: 'none',
        upper: 'upper1',
        lower: 'lower1',
        hat: 'none',
        shoe: 'shoe1',
        back: 'none',
        pet: 'none',
        art1: 'none', // 기부처 1 (월 데코 단일 장착)
        art2: 'none', // 기부처 2 (트리 데코 단일 장착)
    });

    const updateEquip = (category, itemId) => {
        if (itemId === undefined) return;
        setEquipState(prev => {
            let nextState = { ...prev };

            if (nextState[category] === itemId) {
                // 이미 장착된 동일 아이템 클릭 시 해제 (토글)
                if (category === 'upper') {
                    nextState.upper = 'upper1';
                } else if (category === 'lower') {
                    nextState.lower = 'lower1';
                } else if (category === 'outfit') {
                    nextState.outfit = 'none';
                    nextState.upper = 'upper1';
                    nextState.lower = 'lower1';
                } else {
                    nextState[category] = 'none';
                }
            } else {
                nextState[category] = itemId;

                if (itemId !== 'none') {
                    // 한벌옷 장착 시 상/하/신발 벗기기
                    if (category === 'outfit') {
                        nextState.upper = 'none';
                        nextState.lower = 'none';
                        nextState.shoe = 'none';
                    }
                    // 단품 장착 시 한벌옷 벗기기 (이 때 알몸 방지)
                    else if (['upper', 'lower', 'shoe'].includes(category)) {
                        if (nextState.outfit !== 'none') {
                            nextState.outfit = 'none';
                            // 방금 입은 부위가 아닌 나머지 핵심 부위들은 기본옷으로 채워놓기
                            if (category !== 'upper') nextState.upper = 'upper1';
                            if (category !== 'lower') nextState.lower = 'lower1';
                        }
                    }
                } else if (itemId === 'none') {
                    // 명시적 '해제(X)' 버튼 클릭 시 알몸 방지
                    if (category === 'upper') {
                        nextState.upper = 'upper1';
                    } else if (category === 'lower') {
                        nextState.lower = 'lower1';
                    } else if (category === 'outfit') {
                        nextState.outfit = 'none';
                        nextState.upper = 'upper1';
                        nextState.lower = 'lower1';
                        nextState.shoe = 'none';
                    }
                }
            }
            return nextState;
        });
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
                    // 프론트의 모든 카테고리를 뒤져서 유연하게 이름이 포함되는 아이템을 찾아 장착
                    for (const cat in AVATAR_ITEMS) {
                        const matching = AVATAR_ITEMS[cat].find(i =>
                            backendItem.name.includes(i.name) || i.name.includes(backendItem.name)
                        );
                        if (matching) {
                            newEquip[cat] = matching.id;
                            hasChanges = true;
                            break;
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
