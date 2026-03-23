import React, { forwardRef } from 'react';
import { TextInput, Platform, StyleSheet } from 'react-native';
import useAuthStore from '../../store/useAuthStore';

const CustomTextInput = forwardRef((props, ref) => {
    const role = useAuthStore(state => state.user?.role);
    const isParent = role === 'PARENT';

    // 스타일 평탄화로 부모 폰트 웨이트 판별
    const flattenStyle = StyleSheet.flatten(props.style || {});
    const isBold = flattenStyle.fontWeight === 'bold' ||
        flattenStyle.fontWeight === '900' ||
        flattenStyle.fontWeight === '800' ||
        flattenStyle.fontWeight === '700' ||
        flattenStyle.fontWeight >= 600;

    const parentFontFamily = isBold ? 'Pretendard-Bold' : 'Pretendard-Regular';

    return (
        <TextInput
            {...props}
            ref={ref}
            allowFontScaling={false}
            style={[
                isParent ? { fontFamily: parentFontFamily } : styles.defaultFont,
                props.style,
                (!isParent && Platform.OS === 'android') ? { fontWeight: 'normal', fontStyle: 'normal' } : {},
                (isParent && Platform.OS === 'android') ? { fontWeight: 'normal', fontStyle: 'normal' } : {}
            ]}
        />
    );
});

const styles = StyleSheet.create({
    defaultFont: {
        fontFamily: 'Mulmaru',
    }
});

export default CustomTextInput;
