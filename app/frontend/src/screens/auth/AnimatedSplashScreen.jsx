import React, { useEffect } from 'react';
import { View, StyleSheet, Image, Dimensions } from 'react-native';

const { width } = Dimensions.get('window');

const AnimatedSplashScreen = ({ navigation }) => {
    useEffect(() => {
        // 하얀 화면에 로고만 딱 2초 띄워두고 다음으로 넘어갑니다.
        const timer = setTimeout(() => {
            navigation.replace('OnboardingTutorial');
        }, 3000);

        return () => clearTimeout(timer);
    }, [navigation]);

    return (
        <View style={styles.container}>
            <Image
                source={require('../../assets/croco/logo.png')}
                style={styles.logo}
                resizeMode="contain"
            />
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#FFFFFF', // 순백색 배경
        alignItems: 'center',
        justifyContent: 'center',
    },
    logo: {
        width: width * 0.45, // 화면 45% 크기로 대폭 축소
        height: width * 0.45,
    }
});

export default AnimatedSplashScreen;
