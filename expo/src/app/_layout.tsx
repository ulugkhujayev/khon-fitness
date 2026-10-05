import { useEffect } from 'react';
import { useFonts } from 'expo-font';
import { DarkTheme, Stack, ThemeProvider } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import 'react-native-reanimated';

import { Font, K } from '@/theme';

export { ErrorBoundary } from 'expo-router';

export const unstable_settings = { initialRouteName: '(tabs)' };

SplashScreen.preventAutoHideAsync();

/** Dark only (SPEC.md). Navigation colors follow the HIG palette. */
const theme = {
  ...DarkTheme,
  colors: {
    ...DarkTheme.colors,
    primary: K.Accent,
    background: K.Bg,
    card: K.Surface,
    text: K.Text,
    border: K.Divider,
    notification: K.Red,
  },
};

export default function RootLayout() {
  const [loaded, error] = useFonts({
    [Font.regular]: require('../../assets/fonts/inter_regular.ttf'),
    [Font.medium]: require('../../assets/fonts/inter_medium.ttf'),
    [Font.semibold]: require('../../assets/fonts/inter_semibold.ttf'),
    [Font.bold]: require('../../assets/fonts/inter_bold.ttf'),
  });

  useEffect(() => {
    if (error) throw error;
  }, [error]);

  useEffect(() => {
    if (loaded) SplashScreen.hideAsync();
  }, [loaded]);

  if (!loaded) return null;

  return (
    <SafeAreaProvider>
      <ThemeProvider value={theme}>
        <StatusBar style="light" />
        <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: K.Bg } }}>
          <Stack.Screen name="(tabs)" />
        </Stack>
      </ThemeProvider>
    </SafeAreaProvider>
  );
}
