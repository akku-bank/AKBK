import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Platform, Image } from 'react-native';
import ParentHomeScreen from '../screens/parent/home/ParentHomeScreen';
import ParentChallengeManageScreen from '../screens/parent/mission/ParentChallengeManageScreen';
import ParentMyPageScreen from '../screens/parent/mypage/ParentMyPageScreen';

const Tab = createBottomTabNavigator();

const ParentBottomTabNavigator = () => {
    return (
        <Tab.Navigator detachInactiveScreens={false}
            initialRouteName="Home"
            screenOptions={({ route }) => ({
                tabBarIcon: ({ focused, color, size }) => {
                    let iconSource;
                    if (route.name === 'Home') iconSource = require('../assets/icon/home.png');
                    else if (route.name === 'Mission') iconSource = require('../assets/icon/challenge.png');
                    else if (route.name === 'MyPage') iconSource = require('../assets/icon/my.png');

                    return (
                        <Image
                            source={iconSource}
                            style={{ width: 28, height: 28, opacity: focused ? 1 : 0.35 }}
                            resizeMode="contain"
                        />
                    );
                },
                tabBarActiveTintColor: '#000000',
                tabBarInactiveTintColor: '#9CA3AF',
                tabBarStyle: {
                    backgroundColor: '#FFFFFF',
                    borderTopColor: '#E5E7EB',
                    height: Platform.OS === 'ios' ? 90 : 70,
                    paddingBottom: Platform.OS === 'ios' ? 30 : 10,
                    paddingTop: 10,
                },
                tabBarItemStyle: {
                    justifyContent: 'center',
                    alignItems: 'center',
                },
                tabBarLabelStyle: {
                    fontFamily: 'Pretendard-Bold',
                    fontSize: 12,
                    ...(Platform.OS === 'android' ? { fontWeight: 'normal', fontStyle: 'normal' } : {}),
                    marginTop: 4,
                },
                tabBarShowLabel: true,
                headerShown: false,
            })}
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
