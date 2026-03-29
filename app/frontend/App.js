import React, { useEffect } from 'react';
import { View, StyleSheet, Platform, StatusBar } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { useFonts } from 'expo-font';
import * as SplashScreen from 'expo-splash-screen';

import * as Notifications from 'expo-notifications';
import { DeviceEventEmitter } from 'react-native';

import ChildBottomTabNavigator from './src/navigation/ChildBottomTabNavigator';
import AvatarCustomScreen from './src/screens/child/home/AvatarCustomScreen';
import AvatarDictionaryScreen from './src/screens/child/home/AvatarDictionaryScreen';
import TransactionCalendarScreen from './src/screens/child/account/TransactionCalendarScreen';
import TransactionDetailScreen from './src/screens/child/account/TransactionDetailScreen';
import TransferScreen from './src/screens/child/account/TransferScreen';
import PaymentScreen from './src/screens/child/account/PaymentScreen';
import CardListScreen from './src/screens/child/account/CardListScreen';
import CardProductScreen from './src/screens/child/account/CardProductScreen';
import QuizScreen from './src/screens/child/challenge/QuizScreen';
import QuizDifficultySelectScreen from './src/screens/child/challenge/QuizDifficultySelectScreen';
import ChallengeProposeScreen from './src/screens/child/challenge/ChallengeProposeScreen';
import ChallengeDetailScreen from './src/screens/child/challenge/ChallengeDetailScreen';
import WeeklyReportScreen from './src/screens/child/report/WeeklyReportScreen';
import ChildMyPageScreen from './src/screens/child/mypage/ChildMyPageScreen';
import ParentHomeScreen from './src/screens/parent/home/ParentHomeScreen';
import ParentTransferScreen from './src/screens/parent/transfer/ParentTransferScreen';
import ParentReportScreen from './src/screens/parent/report/ParentReportScreen';
import MissionApprovalScreen from './src/screens/parent/mission/MissionApprovalScreen';
import ParentChallengeManageScreen from './src/screens/parent/mission/ParentChallengeManageScreen';
import FamilyManagementScreen from './src/screens/parent/home/FamilyManagementScreen';
import { AvatarProvider } from './src/components/child/avatar/AvatarContext';

import AnimatedSplashScreen from './src/screens/auth/AnimatedSplashScreen';
import OnboardingTutorialScreen from './src/screens/auth/OnboardingTutorialScreen';
import FamilyInvitationScreen from './src/screens/auth/FamilyInvitationScreen';
import SocialLoginScreen from './src/screens/auth/SocialLoginScreen';
import RoleSelectScreen from './src/screens/auth/RoleSelectScreen';
import QrCheckScreen from './src/screens/auth/QrCheckScreen';
import SignUpScreen from './src/screens/auth/SignUpScreen';
import ParentInitialSetupScreen from './src/screens/auth/ParentInitialSetupScreen';
import PinNumberLoginScreen from './src/screens/auth/PinNumberLoginScreen';
import PinNumberSetupScreen from './src/screens/auth/PinNumberSetupScreen';
import QRScanScreen from './src/screens/auth/QRScanScreen';
import ChildFamilyJoinScreen from './src/screens/auth/ChildFamilyJoinScreen';
import ParentFamilyJoinScreen from './src/screens/auth/ParentFamilyJoinScreen';

import ParentBottomTabNavigator from './src/navigation/ParentBottomTabNavigator';
import FamilyQrGeneratorScreen from './src/screens/parent/family/FamilyQrGeneratorScreen';

import ParentEditProfileScreen from './src/screens/parent/mypage/ParentEditProfileScreen';
import ChildEditProfileScreen from './src/screens/child/mypage/ChildEditProfileScreen';
import ParentAccountCreateScreen from './src/screens/parent/transfer/ParentAccountCreateScreen';
import ParentHistoryScreen from './src/screens/parent/account/ParentHistoryScreen';
import GachaScreen from './src/screens/child/safeBox/GachaScreen';
import FriendListScreen from './src/screens/child/social/FriendListScreen';
import FriendTownScreen from './src/screens/child/social/FriendTownScreen';
import FriendSuccessScreen from './src/screens/child/social/FriendSuccessScreen';
import FriendAlreadyScreen from './src/screens/child/social/FriendAlreadyScreen';
import AttendanceScreen from './src/screens/child/challenge/AttendanceScreen';
import ESGChallengeScreen from './src/screens/child/challenge/ESGChallengeScreen';
import BadgeMapScreen from './src/screens/child/safeBox/BadgeMapScreen';
import ChildChangePasswordScreen from './src/screens/child/mypage/ChildChangePasswordScreen';
import ParentChildEditScreen from './src/screens/parent/family/ParentChildEditScreen';
import ParentChangePasswordScreen from './src/screens/parent/mypage/ParentChangePasswordScreen';

// 앱이 포그라운드(실행 중)일 때도 알림 배너가 뜨도록 설정
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
  }),
});

SplashScreen.preventAutoHideAsync().catch(() => { });

const Stack = createNativeStackNavigator();

export default function App() {
  const [fontsLoaded] = useFonts({
    'Mulmaru': require('./src/assets/Mulmaru.ttf'),
  });

  useEffect(() => {
    // 알림 수신 리스너 등록
    const subscription = Notifications.addNotificationReceivedListener(notification => {
      console.log('🔔 알림 수신:', notification.request.content.title);
      // 알림이 오면 전역적으로 내역 갱신 이벤트 발생
      DeviceEventEmitter.emit('refresh_transactions');
    });

    if (fontsLoaded) {
      SplashScreen.hideAsync();

      if (Platform.OS === 'web') {
        if (!document.getElementById('global-font-style')) {
          const style = document.createElement('style');
          style.id = 'global-font-style';
          style.appendChild(document.createTextNode(`
            * {
              font-family: 'Mulmaru', sans-serif !important;
            }
          `));
          document.head.appendChild(style);
        }
      }
    }

    return () => subscription.remove();
  }, [fontsLoaded]);

  if (!fontsLoaded) return null;

  return (
    <View style={styles.webRoot}>
      <StatusBar translucent={false} backgroundColor="#FFFFFF" barStyle="dark-content" />
      <SafeAreaProvider style={{ flex: 1 }}>
        <AvatarProvider>
          <NavigationContainer>
            <Stack.Navigator screenOptions={{ headerShown: false, freezeOnBlur: false, animationEnabled: false }} detachInactiveScreens={false} initialRouteName="AnimatedSplash">
              <Stack.Screen name="AnimatedSplash" component={AnimatedSplashScreen} />
              <Stack.Screen name="OnboardingTutorial" component={OnboardingTutorialScreen} />
              <Stack.Screen name="FamilyInvitation" component={FamilyInvitationScreen} />
              <Stack.Screen name="SocialLogin" component={SocialLoginScreen} />
              <Stack.Screen name="RoleSelect" component={RoleSelectScreen} />
              <Stack.Screen name="QrCheck" component={QrCheckScreen} />
              <Stack.Screen name="SignUp" component={SignUpScreen} />
              <Stack.Screen name="ParentInitialSetup" component={ParentInitialSetupScreen} />
              <Stack.Screen name="PinNumberLogin" component={PinNumberLoginScreen} />
              <Stack.Screen name="PinNumberSetup" component={PinNumberSetupScreen} />
              <Stack.Screen name="QRScan" component={QRScanScreen} />
              <Stack.Screen name="ChildFamilyJoin" component={ChildFamilyJoinScreen} />
              <Stack.Screen name="ParentFamilyJoin" component={ParentFamilyJoinScreen} />

              <Stack.Screen name="ChildMain" component={ChildBottomTabNavigator} />
              <Stack.Screen name="ParentMain" component={ParentBottomTabNavigator} />
              <Stack.Screen name="FamilyQrGenerator" component={FamilyQrGeneratorScreen} />

              <Stack.Screen name="Wardrobe" component={AvatarCustomScreen} />
              <Stack.Screen name="AvatarDictionaryScreen" component={AvatarDictionaryScreen} />
              <Stack.Screen name="GachaScreen" component={GachaScreen} />
              <Stack.Screen name="FriendList" component={FriendListScreen} />
              <Stack.Screen name="FriendTown" component={FriendTownScreen} />
              <Stack.Screen name="FriendSuccess" component={FriendSuccessScreen} />
              <Stack.Screen name="FriendAlready" component={FriendAlreadyScreen} />
              <Stack.Screen name="AttendanceScreen" component={AttendanceScreen} />
              <Stack.Screen name="ESGChallengeScreen" component={ESGChallengeScreen} />
              <Stack.Screen name="BadgeMap" component={BadgeMapScreen} />

              <Stack.Screen name="TransactionCalendar" component={TransactionCalendarScreen} />
              <Stack.Screen name="TransactionDetail" component={TransactionDetailScreen} />
              <Stack.Screen name="Transfer" component={TransferScreen} />
              <Stack.Screen name="Payment" component={PaymentScreen} />
              <Stack.Screen name="CardListScreen" component={CardListScreen} />
              <Stack.Screen name="CardProductScreen" component={CardProductScreen} />

              <Stack.Screen name="ParentHome" component={ParentHomeScreen} />
              <Stack.Screen name="ParentTransferScreen" component={ParentTransferScreen} />
              <Stack.Screen name="ParentReportScreen" component={ParentReportScreen} />
              <Stack.Screen name="ParentHistoryScreen" component={ParentHistoryScreen} />
              <Stack.Screen name="MissionApprovalScreen" component={MissionApprovalScreen} />
              <Stack.Screen name="ParentChallengeManage" component={ParentChallengeManageScreen} />
              <Stack.Screen name="FamilyManagementScreen" component={FamilyManagementScreen} />

              <Stack.Screen name="QuizScreen" component={QuizScreen} />
              <Stack.Screen name="QuizDifficultySelect" component={QuizDifficultySelectScreen} />
              <Stack.Screen name="ChallengePropose" component={ChallengeProposeScreen} />
              <Stack.Screen name="ChallengeDetail" component={ChallengeDetailScreen} />

              <Stack.Screen name="WeeklyReport" component={WeeklyReportScreen} />

              <Stack.Screen name="ParentEditProfile" component={ParentEditProfileScreen} />
              <Stack.Screen name="ChildEditProfile" component={ChildEditProfileScreen} />
              <Stack.Screen name="ParentAccountCreate" component={ParentAccountCreateScreen} />
              <Stack.Screen name="ChildChangePassword" component={ChildChangePasswordScreen} />
              <Stack.Screen name="ParentChildEdit" component={ParentChildEditScreen} />
              <Stack.Screen name="ParentChangePassword" component={ParentChangePasswordScreen} />
            </Stack.Navigator>
          </NavigationContainer>
        </AvatarProvider>
      </SafeAreaProvider>
    </View>
  );
}

const styles = StyleSheet.create({
  webRoot: {
    flex: 1,
    height: Platform.OS === 'web' ? '100vh' : '100%',
  },
});
