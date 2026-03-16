import React, { useEffect } from 'react';
import { View, StyleSheet, Platform, Text, TextInput } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { useFonts } from 'expo-font';
import * as SplashScreen from 'expo-splash-screen';

import ChildBottomTabNavigator from './src/navigation/ChildBottomTabNavigator';
import AvatarCustomScreen from './src/screens/child/home/AvatarCustomScreen';
import { AvatarProvider } from './src/components/child/avatar/AvatarContext';

SplashScreen.preventAutoHideAsync().catch(() => { });

// 폰트 일괄 적용
if (Text.defaultProps == null) Text.defaultProps = {};
Text.defaultProps.style = { fontFamily: 'Mulmaru' };
if (TextInput.defaultProps == null) TextInput.defaultProps = {};
TextInput.defaultProps.style = { fontFamily: 'Mulmaru' };

const Stack = createNativeStackNavigator();

export default function App() {
  const [fontsLoaded] = useFonts({
    'Mulmaru': require('./src/assets/Mulmaru.ttf'),
  });

  useEffect(() => {
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
  }, [fontsLoaded]);

  if (!fontsLoaded) return null;

  return (
    <View style={styles.webRoot}>
      <SafeAreaProvider style={{ flex: 1 }}>
        <AvatarProvider>
          <NavigationContainer>
            <Stack.Navigator screenOptions={{ headerShown: false }}>
              <Stack.Screen name="MainTab" component={ChildBottomTabNavigator} />
              <Stack.Screen name="Wardrobe" component={AvatarCustomScreen} />
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