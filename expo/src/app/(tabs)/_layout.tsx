import { Tabs } from 'expo-router';
import { FloatingTabBar } from '@/components/layout';
import { K } from '@/theme';

export default function TabLayout() {
  return (
    <Tabs
      tabBar={(props) => <FloatingTabBar {...props} />}
      // Screens shift sideways as the selected capsule slides along the bar.
      screenOptions={{ headerShown: false, sceneStyle: { backgroundColor: K.Bg }, animation: 'shift' }}>
      <Tabs.Screen name="index" options={{ title: 'Today' }} />
      <Tabs.Screen name="history" options={{ title: 'History' }} />
      <Tabs.Screen name="programs" options={{ title: 'Programs' }} />
    </Tabs>
  );
}
