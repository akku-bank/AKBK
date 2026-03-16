import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Platform } from 'react-native';
import ParentHomeScreen from '../screens/parent/home/ParentHomeScreen';
import ParentChallengeManageScreen from '../screens/parent/mission/ParentChallengeManageScreen';
import ParentMyPageScreen from '../screens/parent/mypage/ParentMyPageScreen';

const Tab = createBottomTabNavigator();

const ParentBottomTabNavigator = () => {
    return (
        <Tab.Navigator
            screenOptions={{
                tabBarIcon: () => null,
                tabBarActiveTintColor: '#000000',
                tabBarInactiveTintColor: '#9CA3AF',
                tabBarStyle: {
                    backgroundColor: '#FFFFFF',
                    borderTopColor: '#E5E7EB',
                    height: 80,
                },
                tabBarItemStyle: {
                    justifyContent: 'center',
                    alignItems: 'center',
                    padding: 0,
                    margin: 0,
                },
                tabBarIconStyle: {
                    display: 'none',
                },
                tabBarLabelStyle: {
                    fontFamily: 'Mulmaru',
                    fontSize: 15,
                    ...(Platform.OS === 'android' ? { fontWeight: 'normal', fontStyle: 'normal' } : { fontWeight: 'bold' }),
                    position: 'absolute',
                    top: '50%',
                    transform: [{ translateY: -15 }],
                },
                tabBarShowLabel: true,
                headerShown: false,
            }}
        >
            <Tab.Screen
                name="Mission"
                component={ParentChallengeManageScreen}
                options={{ tabBarLabel: '목표 달성' }}
            />
            <Tab.Screen
                name="Home"
                component={ParentHomeScreen}
                options={{ tabBarLabel: '홈' }}
            />
            <Tab.Screen
                name="MyPage"
                component={ParentMyPageScreen}
                options={{ tabBarLabel: 'My' }}
            />
        </Tab.Navigator>
    );
};

export default ParentBottomTabNavigator;
