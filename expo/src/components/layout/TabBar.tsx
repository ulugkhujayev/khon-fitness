import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { SymbolView, SymbolViewProps } from 'expo-symbols';
import type { ComponentProps } from 'react';
import type { Tabs } from 'expo-router';
import { Font, K, Type } from '@/theme';

/** React Navigation ships inside expo-router in this SDK, so take the tab bar props from Tabs itself. */
type BottomTabBarProps = Parameters<NonNullable<ComponentProps<typeof Tabs>['tabBar']>>[0];

type Item = { label: string; icon: SymbolViewProps['name'] };

const ITEMS: Record<string, Item> = {
  index: { label: 'Today', icon: { ios: 'calendar', android: 'today', web: 'today' } },
  history: { label: 'History', icon: { ios: 'clock.fill', android: 'history', web: 'history' } },
  programs: { label: 'Programs', icon: { ios: 'list.bullet', android: 'fitness_center', web: 'fitness_center' } },
};

/**
 * Floating capsule tab bar (DESIGN.md): 60 pt tall, 24 pt from the sides, 10 pt above the bottom edge,
 * group surface with a hairline and a soft shadow; the selected item sits on a Surface2 capsule tinted accent.
 */
export function FloatingTabBar({ state, navigation }: BottomTabBarProps) {
  const insets = useSafeAreaInsets();
  return (
    <View pointerEvents="box-none" style={[styles.wrap, { paddingBottom: insets.bottom + 10 }]}>
      <View style={styles.bar}>
        {state.routes.map((route, i) => {
          const item = ITEMS[route.name];
          if (!item) return null;
          const active = state.index === i;
          const color = active ? K.Accent : K.Muted;
          return (
            <Pressable
              key={route.key}
              accessibilityRole="tab"
              accessibilityState={{ selected: active }}
              accessibilityLabel={item.label}
              onPress={() => {
                const event = navigation.emit({ type: 'tabPress', target: route.key, canPreventDefault: true });
                if (!active && !event.defaultPrevented) navigation.navigate(route.name);
              }}
              style={[styles.item, active && styles.active]}>
              <SymbolView name={item.icon} tintColor={color} size={24} />
              <Text style={[styles.label, { color }]}>{item.label}</Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { position: 'absolute', left: 0, right: 0, bottom: 0, paddingHorizontal: 24, paddingTop: 6 },
  bar: {
    flexDirection: 'row',
    height: 60,
    borderRadius: 30,
    backgroundColor: K.Surface,
    borderWidth: 1,
    borderColor: K.Hairline,
    padding: 4,
    shadowColor: K.Shadow,
    shadowOpacity: 0.6,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 4 },
    elevation: 12,
  },
  item: { flex: 1, borderRadius: 26, alignItems: 'center', justifyContent: 'center' },
  active: { backgroundColor: K.Surface2 },
  label: { ...Type.caption2, fontFamily: Font.medium, marginTop: 2 },
});
