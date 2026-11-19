import { Link, usePathname, type Href } from 'expo-router';
import { useState } from 'react';
import { Animated, Pressable, StyleSheet, type PressableProps } from 'react-native';

import { MaxContentWidth, Spacing } from '@/constants/theme';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';

const tabs = [
  { label: 'Home', href: '/' as const },
  { label: 'Testing', href: '/explore' as const },
  { label: 'Leaderboard', href: '/leaderboard' as const },
];

export function TopNav() {
  const pathname = usePathname();

  return (
    <ThemedView style={[styles.bar, { borderBottomColor: '#C8D9EF' }]}>
      <ThemedView style={styles.inner}>
        <ThemedText type="smallBold" style={styles.brand}>Hygiene Tracker</ThemedText>
        <ThemedView style={styles.tabs}>
          {tabs.map((tab) => {
            const isActive = tab.href === '/' ? pathname === '/' : pathname.startsWith(tab.href);
            return (
              <Link key={tab.href} href={tab.href as unknown as Href} asChild>
                <NavButton label={tab.label} isActive={isActive} />
              </Link>
            );
          })}
        </ThemedView>
      </ThemedView>
    </ThemedView>
  );
}

function NavButton({ label, isActive, ...pressableProps }: { label: string; isActive: boolean } & PressableProps) {
  const [scale] = useState(() => new Animated.Value(1));
  const [hovered, setHovered] = useState(false);

  const animateScale = (toValue: number) => {
    Animated.spring(scale, {
      toValue,
      useNativeDriver: true,
      speed: 24,
      bounciness: 5,
    }).start();
  };

  return (
    <Pressable
      {...pressableProps}
      accessibilityRole="button"
      accessibilityState={{ selected: isActive }}
      onHoverIn={() => {
        setHovered(true);
        animateScale(1.045);
      }}
      onHoverOut={() => {
        setHovered(false);
        animateScale(1);
      }}
      onPressIn={() => animateScale(0.98)}
      onPressOut={() => animateScale(hovered ? 1.045 : 1)}
      style={({ pressed }) => [styles.tab, pressed && styles.pressed]}>
      <Animated.View
        style={[
          styles.tabOutline,
          isActive ? styles.activeTab : styles.inactiveTab,
          { transform: [{ scale }] },
        ]}>
        <ThemedText type="small" style={{ color: isActive ? '#2563EB' : '#5B7083' }}>
          {label}
        </ThemedText>
      </Animated.View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  bar: {
    width: '100%',
    backgroundColor: '#FFFFFF',
    borderBottomWidth: 1,
  },
  inner: {
    width: '100%',
    backgroundColor: '#FFFFFF',
    maxWidth: MaxContentWidth,
    alignSelf: 'center',
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.four,
    paddingVertical: Spacing.three,
  },
  brand: {
    marginRight: 'auto',
    color: '#102A43',
  },
  tabs: {
    flexDirection: 'row',
    gap: Spacing.two,
    backgroundColor: '#FFFFFF',
  },
  tab: {
    padding: Spacing.one,
  },
  tabOutline: {
    minWidth: 84,
    paddingHorizontal: Spacing.four,
    paddingVertical: Spacing.two,
    borderRadius: Spacing.five,
    borderWidth: 1,
    alignItems: 'center',
  },
  activeTab: {
    backgroundColor: '#EAF2FF',
    borderColor: '#8BB4EA',
  },
  inactiveTab: {
    backgroundColor: '#FFFFFF',
    borderColor: '#C8D9EF',
  },
  pressed: {
    opacity: 0.7,
  },
});
