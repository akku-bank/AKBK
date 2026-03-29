import React, { forwardRef } from 'react';
import { Text, Platform, StyleSheet } from 'react-native';
import useAuthStore from '../../store/useAuthStore';

const CustomText = forwardRef((props, ref) => {
    const role = useAuthStore(state => state.user?.role);
    const isParent = role === 'PARENT';

    // 스타일을 평탄화하여 fontWeight 속성 확인
    const flattenStyle = StyleSheet.flatten(props.style || {});
    const isBold = flattenStyle.fontWeight === 'bold' ||
        flattenStyle.fontWeight === '900' ||
        flattenStyle.fontWeight === '800' ||
        flattenStyle.fontWeight === '700' ||
        flattenStyle.fontWeight >= 600;

    const parentFontFamily = 'Pretendard-Bold';

    return (
        <Text
            {...props}
            ref={ref}
            allowFontScaling={false}
            textBreakStrategy="simple"
            lineBreakStrategyIOS="hangul-word"
            style={[
                isParent ? { fontFamily: parentFontFamily } : styles.defaultFont,
                props.style,
                (!isParent && Platform.OS === 'android') ? { fontWeight: 'normal', fontStyle: 'normal' } : {},
                (isParent && Platform.OS === 'android') ? { fontWeight: 'normal', fontStyle: 'normal' } : {},
                Platform.OS === 'web' ? { wordBreak: 'keep-all', overflowWrap: 'break-word' } : {}
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
