import React, { useEffect } from 'react';
import { View, StyleSheet, Image, Dimensions, TouchableOpacity } from 'react-native';

const { width } = Dimensions.get('window');

const AnimatedSplashScreen = ({ navigation }) => {
    useEffect(() => {
        // 하얀 화면에 로고만 딱 2초 띄워두고 다음으로 넘어갑니다.
        const timer = setTimeout(() => {
            console.log('Splash timer triggered: Navigating to OnboardingTutorial');
            navigation.navigate('OnboardingTutorial');
        }, 3000);

        return () => clearTimeout(timer);
    }, []); // navigation 의존성 제거 (무한 리렌더링 방지)

    return (
        <TouchableOpacity style={styles.container} activeOpacity={1} onPress={() => {
            console.log('Splash screen pressed: Navigating to OnboardingTutorial');
            navigation.navigate('OnboardingTutorial');
        }}>
            <Image
                source={require('../../assets/intro.png')}
                style={styles.logo}
                resizeMode="cover"
            />
        </TouchableOpacity>
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
        width: '100%',
        height: '100%',
    }
});

export default AnimatedSplashScreen;
