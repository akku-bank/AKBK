import React from 'react';
import { View, TouchableOpacity, StyleSheet, Image } from 'react-native';
import { scale, verticalScale } from 'react-native-size-matters';
import CustomText from '../../../components/common/CustomText';

const TABS = [
    { id: 'home', label: '홈', icon: '🏠', route: 'ChildHome' },
    { id: 'shop', label: '도감', icon: '🎒', route: 'AvatarDictionaryScreen' },
    { id: 'account', label: '내 소비', icon: '💸', route: 'ChildAccount' },
    { id: 'mypage', label: '내 정보', icon: null, route: 'ChildMyPage' }
];

const ChildBottomNavigation = ({ navigation, currentTab }) => {
    return (
        <View style={styles.container}>
            {TABS.map(tab => {
                const isActive = currentTab === tab.id;
                return (
                    <TouchableOpacity
                        key={tab.id}
                        style={styles.tabItem}
                        onPress={() => {
                            if (!isActive) {
                                navigation.navigate(tab.route);
                            }
                        }}
                    >
                        {tab.id === 'mypage' ? (
                            <Image
                                source={require('../../../assets/croco/croco_face.png')}
                                style={[styles.iconImage, !isActive && { opacity: 0.5 }]}
                                resizeMode="contain"
                            />
                        ) : (
                            <CustomText style={[styles.icon, isActive && styles.activeIcon]}>
                                {tab.icon}
                            </CustomText>
                        )}
                        <CustomText style={[styles.label, isActive && styles.activeLabel]}>
                            {tab.label}
                        </CustomText>
                    </TouchableOpacity>
                );
            })}
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        flexDirection: 'row',
        backgroundColor: '#FFFFFF',
        position: 'absolute',
        bottom: 0,
        left: 0,
        right: 0,
        paddingBottom: verticalScale(24), // iOS 하단 안전 영역 대응 여백
        paddingTop: verticalScale(12),
        borderTopWidth: 1,
        borderTopColor: '#F3F4F6',
        justifyContent: 'space-around',
        alignItems: 'center',
    },
    tabItem: {
        alignItems: 'center',
        flex: 1,
    },
    icon: {
        fontSize: scale(20),
        marginBottom: verticalScale(4),
        opacity: 0.5,
    },
    iconImage: {
        width: scale(24),
        height: scale(24),
        marginBottom: verticalScale(4),
    },
    activeIcon: {
        opacity: 1.0,
    },
    label: {
        fontSize: scale(10),
        color: '#9CA3AF',
        fontWeight: '600',
    },
    activeLabel: {
        color: '#111',
        fontWeight: 'bold',
    }
});

export default ChildBottomNavigation;
