import React, { forwardRef } from 'react';
import { Text, Platform, StyleSheet } from 'react-native';
import useAuthStore from '../../store/useAuthStore';

const CustomText = forwardRef((props, ref) => {
    const role = useAuthStore(state => state.user?.role);
    const isParent = role === 'PARENT';

    return (
        <Text
            {...props}
            ref={ref}
            allowFontScaling={false}
            style={[
                isParent ? styles.parentFont : styles.defaultFont,
                props.style,
                (!isParent && Platform.OS === 'android') ? { fontWeight: 'normal', fontStyle: 'normal' } : {}
            ]}
        >
            {props.children}
        </Text>
    );
});

const styles = StyleSheet.create({
    defaultFont: {
        fontFamily: 'Mulmaru',
    },
    parentFont: {
        // 시스템 기본 폰트 사용
    }
});

export default CustomText;
