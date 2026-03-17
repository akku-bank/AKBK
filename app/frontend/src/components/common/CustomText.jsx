import React, { forwardRef } from 'react';
import { Text, Platform, StyleSheet } from 'react-native';

const CustomText = forwardRef((props, ref) => {
    return (
        <Text
            {...props}
            ref={ref}
            allowFontScaling={false}
            style={[
                styles.defaultFont,
                props.style,
                Platform.OS === 'android' ? { fontWeight: 'normal', fontStyle: 'normal' } : {}
            ]}
        >
            {props.children}
        </Text>
    );
});

const styles = StyleSheet.create({
    defaultFont: {
        fontFamily: 'Mulmaru',
    }
});

export default CustomText;
