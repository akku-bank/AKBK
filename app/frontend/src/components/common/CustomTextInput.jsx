import React, { forwardRef } from 'react';
import { TextInput, Platform, StyleSheet } from 'react-native';

const CustomTextInput = forwardRef((props, ref) => {
    return (
        <TextInput
            {...props}
            ref={ref}
            allowFontScaling={false}
            style={[
                styles.defaultFont,
                props.style,
                Platform.OS === 'android' ? { fontWeight: 'normal', fontStyle: 'normal' } : {}
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
