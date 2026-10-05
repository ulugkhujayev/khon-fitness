import { Tabs } from 'expo-router';
import { FloatingTabBar } from '@/components/layout';
import { K } from '@/theme';

export default function TabLayout() {
  return (
    <Tabs
      tabBar={(props) => <FloatingTabBar {...props} />}
      screenOptions={{ headerShown: false, sceneStyle: { backgroundColor: K.Bg } }}>
      <Tabs.Screen name="index" options={{ title: 'Today' }} />
      <Tabs.Screen name="history" options={{ title: 'History' }} />
      <Tabs.Screen name="programs" options={{ title: 'Programs' }} />
    </Tabs>
  );
}
