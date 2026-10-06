import { ComponentProps, useEffect, useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import Animated, { useAnimatedStyle, useSharedValue, withSpring } from 'react-native-reanimated';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { SymbolView, SymbolViewProps } from 'expo-symbols';
import type { Tabs } from 'expo-router';
import { Font, K, Type } from '@/theme';

/** React Navigation ships inside expo-router in this SDK, so take the tab bar props from Tabs itself. */
type BottomTabBarProps = Parameters<NonNullable<ComponentProps<typeof Tabs>['tabBar']>>[0];

type Item = { label: string; icon: SymbolViewProps['name'] };

/** The capsule's height and its gap to the bottom edge, which sits above the home indicator. */
const BAR = { height: 60, gap: 10 } as const;

/** What the bar covers above the bottom safe-area inset: scrolling content behind it has to clear this. */
export const TAB_BAR_HEIGHT = BAR.height + BAR.gap;

const ITEMS: Record<string, Item> = {
  index: { label: 'Today', icon: { ios: 'calendar', android: 'today', web: 'today' } },
  history: { label: 'History', icon: { ios: 'clock.fill', android: 'history', web: 'history' } },
  programs: { label: 'Programs', icon: { ios: 'list.bullet', android: 'fitness_center', web: 'fitness_center' } },
};

/** How the selected capsule moves to a new tab: a quick spring that settles without a wobble. */
const SLIDE = { damping: 20, stiffness: 240, mass: 0.8 } as const;

/**
 * Floating capsule tab bar (DESIGN.md): 60 pt tall, 24 pt from the sides, 10 pt above the bottom edge,
 * group surface with a hairline and a soft shadow; the selected item sits on a Surface2 capsule tinted accent,
 * and that capsule slides to the tab tapped instead of jumping there.
 */
export function FloatingTabBar({ state, navigation }: BottomTabBarProps) {
  const insets = useSafeAreaInsets();
  const [width, setWidth] = useState(0);
  const slot = width > 0 ? width / state.routes.length : 0;
  const x = useSharedValue(0);
  // The first place is set at once; only later changes slide.
  const placed = useRef(false);

  useEffect(() => {
    if (slot === 0) return;
    const target = state.index * slot;
    x.set(placed.current ? withSpring(target, SLIDE) : target);
    placed.current = true;
  }, [state.index, slot, x]);

  const capsule = useAnimatedStyle(() => ({ transform: [{ translateX: x.get() }] }));

  return (
    <View pointerEvents="box-none" style={[styles.wrap, { paddingBottom: insets.bottom + BAR.gap }]}>
      <View style={styles.bar}>
        <View style={styles.items} onLayout={(e) => setWidth(e.nativeEvent.layout.width)}>
          {slot > 0 ? <Animated.View style={[styles.capsule, { width: slot }, capsule]} /> : null}
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
                style={styles.item}>
                <SymbolView name={item.icon} tintColor={color} size={24} />
                <Text style={[styles.label, { color }]}>{item.label}</Text>
              </Pressable>
            );
          })}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { position: 'absolute', left: 0, right: 0, bottom: 0, paddingHorizontal: 24, paddingTop: 6 },
  bar: {
    height: BAR.height,
    borderRadius: BAR.height / 2,
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
  // The items' own row, with no padding, so a slot is exactly a share of its width.
  items: { flex: 1, flexDirection: 'row' },
  capsule: { position: 'absolute', top: 0, bottom: 0, left: 0, borderRadius: 26, backgroundColor: K.Surface2 },
  item: { flex: 1, borderRadius: 26, alignItems: 'center', justifyContent: 'center' },
  label: { ...Type.caption2, fontFamily: Font.medium, marginTop: 2 },
});
