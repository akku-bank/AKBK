import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { View, Text } from 'react-native';
import ChildHomeScreen from '../screens/child/home/ChildHomeScreen';
import ChildChallengeScreen from '../screens/child/challenge/ChildChallengeScreen';
import ChildSafeBoxScreen from '../screens/child/safeBox/ChildSafeBoxScreen';
import ChildAccountScreen from '../screens/child/account/ChildAccountScreen';
import ChildMyPageScreen from '../screens/child/mypage/ChildMyPageScreen';

const Tab = createBottomTabNavigator();

const PlaceholderScreen = ({ name }) => (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#F3F4F6' }}>
        <Text style={{ fontSize: 20, fontWeight: 'bold' }}>{name}</Text>
    </View>
);

const ChildBottomTabNavigator = () => {
    return (
        <Tab.Navigator
            screenOptions={{
                tabBarIcon: () => null,
                tabBarActiveTintColor: '#000000ff',
                tabBarInactiveTintColor: '#9CA3AF',
                tabBarStyle: {
                    backgroundColor: '#FFFFFF',
                    borderTopColor: '#E5E7EB',
                    height: 60,
                },
                tabBarItemStyle: {
                    justifyContent: 'center',
                    alignItems: 'center',
                    padding: 0,
                    margin: 0,
                },
                tabBarLabelStyle: {
                    fontSize: 14,
                    fontWeight: 'bold',
                    position: 'absolute',
                    top: '50%',
                    transform: [{ translateY: -10 }],
                },
                tabBarShowLabel: true,
                headerShown: false,
            }}
        >
            <Tab.Screen
                name="Challenge"
                component={ChildChallengeScreen}
                options={{ tabBarLabel: '챌린지' }}
            />
            <Tab.Screen
                name="SafeBox"
                component={ChildSafeBoxScreen}
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
