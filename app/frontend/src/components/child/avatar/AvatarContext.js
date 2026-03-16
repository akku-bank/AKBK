import React, { createContext, useState } from 'react';

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

    return (
        <AvatarContext.Provider value={{ equipState, updateEquip, setEquipState }}>
            {children}
        </AvatarContext.Provider>
    );
};
