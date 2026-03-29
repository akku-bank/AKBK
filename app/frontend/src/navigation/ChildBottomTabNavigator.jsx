import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { View, Text, Platform, Image } from 'react-native';
import ChildHomeScreen from '../screens/child/home/ChildHomeScreen';
import ChallengeScreen from '../screens/child/challenge/ChallengeScreen';
import SafeBoxScreen from '../screens/child/safeBox/SafeBoxScreen';
import ChildAccountScreen from '../screens/child/account/ChildAccountScreen';
import ChildMyPageScreen from '../screens/child/mypage/ChildMyPageScreen';

const Tab = createBottomTabNavigator();

const PlaceholderScreen = ({ name }) => (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#F9FAFB' }}>
        <Text style={{ fontSize: 20, fontWeight: 'bold' }}>{name}</Text>
    </View>
);

const ChildBottomTabNavigator = () => {
    return (
        <Tab.Navigator detachInactiveScreens={false}
            initialRouteName="Home"
            screenOptions={({ route }) => ({
                tabBarIcon: ({ focused, color, size }) => {
                    let iconSource;
                    if (route.name === 'Home') iconSource = require('../assets/icon/home.png');
                    else if (route.name === 'Challenge') iconSource = require('../assets/icon/challenge.png');
                    else if (route.name === 'SafeBox') iconSource = require('../assets/icon/jelling.png');
                    else if (route.name === 'Account') iconSource = require('../assets/icon/pay.png');
                    else if (route.name === 'MyPage') iconSource = require('../assets/icon/my.png');

                    return (
                        <Image
                            source={iconSource}
                            style={{ width: 28, height: 28, opacity: focused ? 1 : 0.35 }}
                            resizeMode="contain"
                        />
                    );
                },
                tabBarActiveTintColor: '#000000ff',
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
                    fontFamily: 'Mulmaru',
                    fontSize: 12,
                    ...(Platform.OS === 'android' ? { fontWeight: 'normal', fontStyle: 'normal' } : { fontWeight: 'bold' }),
                    marginTop: 4,
                },
                tabBarShowLabel: true,
                headerShown: false,
            })}
        >
            <Tab.Screen
                name="Challenge"
                component={ChallengeScreen}
                options={{ tabBarLabel: '챌린지' }}
            />
            <Tab.Screen
                name="SafeBox"
                component={SafeBoxScreen}
                options={{ tabBarLabel: '젤링' }}
            />
            <Tab.Screen
                name="Home"
                component={ChildHomeScreen}
                options={{ tabBarLabel: '홈' }}
            />
            <Tab.Screen
                name="Account"
                component={ChildAccountScreen}
                options={{ tabBarLabel: '결제' }}
            />
            <Tab.Screen
                name="MyPage"
                component={ChildMyPageScreen}
                options={{ tabBarLabel: 'My' }}
            />
        </Tab.Navigator>
    );
};

export default ChildBottomTabNavigator;
