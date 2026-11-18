import { usePathname, useRouter } from 'expo-router';
import { PropsWithChildren, ReactNode, useEffect, useState } from 'react';
import { Animated, Pressable, PressableProps, StyleProp, StyleSheet, View, ViewStyle } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAuth } from '@/context/auth-context';
import { Spacing } from '@/constants/theme';
import { ThemedText } from '@/components/themed-text';
import { useTheme } from '@/hooks/use-theme';

const sidebarBubbles = [
  { left: '12%', size: 12, delay: 0, duration: 5200 },
  { left: '35%', size: 8, delay: 900, duration: 4600 },
  { left: '58%', size: 16, delay: 1700, duration: 5800 },
  { left: '82%', size: 10, delay: 500, duration: 4900 },
];

const AnimatedPressable = Animated.createAnimatedComponent(Pressable);

function SidebarButton({ children, style, ...props }: PressableProps & { children: ReactNode; style?: StyleProp<ViewStyle> }) {
  const [scale] = useState(() => new Animated.Value(1));
  const [hovered, setHovered] = useState(false);

  const animateScale = (toValue: number) => {
    Animated.spring(scale, { toValue, useNativeDriver: true, speed: 28, bounciness: 4 }).start();
  };

  return (
    <AnimatedPressable
      {...props}
      onHoverIn={() => {
        setHovered(true);
        animateScale(1.03);
      }}
      onHoverOut={() => {
        setHovered(false);
        animateScale(1);
      }}
      onPressIn={() => animateScale(0.97)}
      onPressOut={() => animateScale(hovered ? 1.03 : 1)}
      style={[style, { transform: [{ scale }] }]}
    >
      {children}
    </AnimatedPressable>
  );
}

export function AppShell({ children }: PropsWithChildren) {
  const router = useRouter();
  const pathname = usePathname();
  const { user, signOut } = useAuth();
  const theme = useTheme();
  const [bubbleProgress] = useState(() => sidebarBubbles.map(() => new Animated.Value(0)));

  useEffect(() => {
    const animations = bubbleProgress.map((progress, index) => {
      const bubble = sidebarBubbles[index];
      return Animated.loop(
        Animated.sequence([
          Animated.timing(progress, { toValue: 0, duration: 0, useNativeDriver: true }),
          Animated.delay(bubble.delay),
          Animated.timing(progress, { toValue: 1, duration: bubble.duration, useNativeDriver: true }),
        ])
      );
    });

    animations.forEach((animation) => animation.start());
    return () => animations.forEach((animation) => animation.stop());
  }, [bubbleProgress]);

  const handleSignOut = async () => {
    await signOut();
    router.replace('/login');
  };

  return (
    <SafeAreaView style={[styles.safeArea, { backgroundColor: theme.background }]}>
      <View style={styles.layout}>
        <View style={styles.sidebar}>
          <View pointerEvents="none" style={styles.bubbleLayer}>
            {sidebarBubbles.map((bubble, index) => {
              const progress = bubbleProgress[index];
              return (
                <Animated.View
                  key={`${bubble.left}-${bubble.size}`}
                  style={[
                    styles.bubble,
                    { left: bubble.left, width: bubble.size, height: bubble.size, borderRadius: bubble.size / 2 },
                    {
                      opacity: progress.interpolate({ inputRange: [0, 0.12, 0.82, 1], outputRange: [0, 0.32, 0.22, 0] }),
                      transform: [{ translateY: progress.interpolate({ inputRange: [0, 1], outputRange: [-24, 460] }) }],
                    },
                  ]}
                />
              );
            })}
          </View>
          <View style={styles.profileBlock}>
            <View style={styles.avatar}>
              <ThemedText style={styles.avatarText}>
                {(user?.displayName?.trim() || 'U').slice(0, 1).toUpperCase()}
              </ThemedText>
            </View>
            <ThemedText type="smallBold" numberOfLines={2} style={styles.displayName}>
              {user?.displayName?.trim() || 'Your profile'}
            </ThemedText>
          </View>

          <View style={styles.navigation}>
            <SidebarButton
              accessibilityRole="button"
              accessibilityState={{ selected: pathname === '/' }}
              onPress={() => router.replace('/')}
              style={[styles.navigationItem, pathname === '/' && styles.selectedItem]}>
              <ThemedText type="small" style={[styles.navigationText, pathname === '/' && styles.selectedText]}>Home</ThemedText>
            </SidebarButton>
            <SidebarButton
              accessibilityRole="button"
              accessibilityState={{ selected: pathname === '/explore' }}
              onPress={() => router.replace('/explore')}
              style={[styles.navigationItem, pathname === '/explore' && styles.selectedItem]}>
              <ThemedText type="small" style={[styles.navigationText, pathname === '/explore' && styles.selectedText]}>Testing</ThemedText>
            </SidebarButton>
          </View>

          <SidebarButton
            accessibilityRole="button"
            accessibilityLabel="Sign out"
            onPress={() => void handleSignOut()}
            style={styles.signOut}>
            <ThemedText type="small" style={styles.signOutText}>Sign out</ThemedText>
          </SidebarButton>
        </View>

        <View style={styles.content}>
          <View style={styles.main}>{children}</View>
          <View style={[styles.footer, { borderTopColor: '#D9DEE8' }]}>
            <ThemedText type="small" themeColor="textSecondary">© 2026 Hygiene Tracker</ThemedText>
            <ThemedText type="small" themeColor="textSecondary">CSUMB School of Computing & Design</ThemedText>
          </View>
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1 },
  layout: { flex: 1, flexDirection: 'row' },
  sidebar: { width: 240, padding: Spacing.four, justifyContent: 'space-between', backgroundColor: '#FFFFFF', borderRightWidth: 1, borderRightColor: '#C8D9EF', overflow: 'hidden' },
  bubbleLayer: { position: 'absolute', top: 0, right: 0, bottom: 0, left: 0, overflow: 'hidden' },
  bubble: { position: 'absolute', top: 0, backgroundColor: '#A4C7F5', shadowColor: '#FFFFFF', shadowOpacity: 0.55, shadowRadius: 4, elevation: 2 },
  profileBlock: { alignItems: 'center', gap: Spacing.two, padding: Spacing.three, borderRadius: 18, backgroundColor: '#EAF2FF' },
  avatar: { width: 52, height: 52, borderRadius: 26, alignItems: 'center', justifyContent: 'center', backgroundColor: '#DCEAFF' },
  avatarText: { color: '#2563EB', fontSize: 20, fontWeight: '800' },
  displayName: { textAlign: 'center', color: '#102A43' },
  navigation: { gap: Spacing.one, alignSelf: 'stretch' },
  navigationItem: { paddingHorizontal: Spacing.three, paddingVertical: Spacing.two + 2, borderRadius: 12 },
  navigationText: { color: '#102A43' },
  selectedItem: { backgroundColor: '#EAF2FF', borderLeftWidth: 3, borderLeftColor: '#2563EB' },
  selectedText: { color: '#2563EB', fontWeight: '700' },
  signOut: { alignSelf: 'stretch', alignItems: 'center', paddingHorizontal: Spacing.three, paddingVertical: Spacing.two, borderRadius: 24, borderWidth: 1, borderColor: '#B42318' },
  signOutText: { color: '#B42318', fontWeight: '700' },
  content: { flex: 1 },
  main: { flex: 1 },
  footer: { borderTopWidth: 1, paddingHorizontal: Spacing.four, paddingVertical: Spacing.three, gap: Spacing.one },
  pressed: { opacity: 0.7 },
});
