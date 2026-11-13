import * as Google from 'expo-auth-session/providers/google';
import * as WebBrowser from 'expo-web-browser';
import { useAuthRequest } from 'expo-auth-session';
import { router } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Animated,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  Image,
  useWindowDimensions,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAuth } from '@/context/auth-context';
import { exchangeGithubCode, githubRedirectUri } from '@/services/github-auth';
import { signInWithGoogle } from '@/services/google-auth';

WebBrowser.maybeCompleteAuthSession();

const webClientId = process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID;
const androidClientId = process.env.EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID;
const iosClientId = process.env.EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID;
const githubClientId = process.env.EXPO_PUBLIC_GITHUB_CLIENT_ID;
const googleConfigured = Boolean(webClientId || androidClientId || iosClientId);
const githubConfigured = Boolean(githubClientId);
const githubDiscovery = {
  authorizationEndpoint: 'https://github.com/login/oauth/authorize',
};
// Add, remove, or edit messages here. One message is selected randomly per visit.
const loginEyebrowMessages = [
  'Time to shower, you punk rocker',
  'To be in STEM is not to SMELL',
  'Mr. Clean will be proud of you',
  'League player?'
];
const waterfallBubbleConfigs = [
  { left: '10%', size: 14, delay: 0, duration: 5600 },
  { left: '24%', size: 9, delay: 900, duration: 4800 },
  { left: '39%', size: 18, delay: 1700, duration: 6200 },
  { left: '58%', size: 11, delay: 500, duration: 5100 },
  { left: '73%', size: 16, delay: 2300, duration: 5900 },
  { left: '87%', size: 8, delay: 1300, duration: 4500 },
];

const colors = {
  ink: '#102A43', muted: '#5B7083', background: '#E8F1FF', surface: '#FFFFFF',
  paleBlue: '#EAF2FF', blue: '#2563EB', line: '#C8D9EF', sky: '#A4C7F5',
};

export default function LoginScreen() {
  const { signIn } = useAuth();
  const { width } = useWindowDimensions();
  const isWide = width >= 840;
  const [eyebrowMessage] = useState(
    () => loginEyebrowMessages[Math.floor(Math.random() * loginEyebrowMessages.length)]
  );
  const pulse = useRef(new Animated.Value(0)).current;
  const formEntrance = useRef(new Animated.Value(0)).current;
  const githubEntrance = useRef(new Animated.Value(0)).current;
  const googleEntrance = useRef(new Animated.Value(0)).current;
  const githubScale = useRef(new Animated.Value(1)).current;
  const googleScale = useRef(new Animated.Value(1)).current;
  const githubHovered = useRef(false);
  const googleHovered = useRef(false);
  const waterfallProgress = useRef(waterfallBubbleConfigs.map(() => new Animated.Value(0))).current;

  useEffect(() => {
    const pulseAnimation = Animated.loop(
      Animated.sequence([
        Animated.timing(pulse, {
          toValue: 1,
          duration: 3200,
          useNativeDriver: true,
        }),
        Animated.timing(pulse, {
          toValue: 0,
          duration: 3200,
          useNativeDriver: true,
        }),
      ])
    );

    pulseAnimation.start();
    const entranceAnimation = Animated.stagger(130, [
      Animated.timing(formEntrance, {
        toValue: 1,
        duration: 500,
        useNativeDriver: true,
      }),
      Animated.timing(githubEntrance, {
        toValue: 1,
        duration: 400,
        useNativeDriver: true,
      }),
      Animated.timing(googleEntrance, {
        toValue: 1,
        duration: 400,
        useNativeDriver: true,
      }),
    ]);
    const waterfallAnimations = waterfallProgress.map((progress, index) => {
      const { delay, duration } = waterfallBubbleConfigs[index];
      return Animated.loop(
        Animated.sequence([
          Animated.timing(progress, { toValue: 0, duration: 0, useNativeDriver: true }),
          Animated.delay(delay),
          Animated.timing(progress, { toValue: 1, duration, useNativeDriver: true }),
        ])
      );
    });

    entranceAnimation.start();
    waterfallAnimations.forEach((animation) => animation.start());
    return () => {
      pulseAnimation.stop();
      entranceAnimation.stop();
      waterfallAnimations.forEach((animation) => animation.stop());
    };
  }, [formEntrance, githubEntrance, googleEntrance, pulse, waterfallProgress]);

  const pulseStyle = {
    opacity: pulse.interpolate({ inputRange: [0, 1], outputRange: [0.28, 0.5] }),
    transform: [{ scale: pulse.interpolate({ inputRange: [0, 1], outputRange: [0.92, 1.08] }) }],
  };
  const formEntranceStyle = {
    opacity: formEntrance,
    transform: [{ translateY: formEntrance.interpolate({ inputRange: [0, 1], outputRange: [24, 0] }) }],
  };
  const githubEntranceStyle = {
    opacity: githubEntrance,
    transform: [
      { translateY: githubEntrance.interpolate({ inputRange: [0, 1], outputRange: [14, 0] }) },
      { scale: githubScale },
    ],
  };
  const googleEntranceStyle = {
    opacity: googleEntrance,
    transform: [
      { translateY: googleEntrance.interpolate({ inputRange: [0, 1], outputRange: [14, 0] }) },
      { scale: googleScale },
    ],
  };

  const animateButtonPress = (scale: Animated.Value, toValue: number) => {
    Animated.spring(scale, { toValue, useNativeDriver: true, speed: 24, bounciness: 4 }).start();
  };
  const [googleRequest, , promptGoogleAsync] = Google.useIdTokenAuthRequest({
    androidClientId: androidClientId || webClientId || 'missing.apps.googleusercontent.com',
    iosClientId: iosClientId || webClientId || 'missing.apps.googleusercontent.com',
    webClientId: webClientId || 'missing.apps.googleusercontent.com',
    scopes: ['openid', 'email', 'profile'],
    selectAccount: true,
  });
  const [githubRequest, , promptGithubAsync] = useAuthRequest(
    {
      clientId: githubClientId || 'missing-github-client-id',
      scopes: ['read:user', 'user:email'],
      redirectUri: githubRedirectUri,
      usePKCE: true,
    },
    githubDiscovery
  );
  const [loadingProvider, setLoadingProvider] = useState<'google' | 'github' | null>(null);
  const [authError, setAuthError] = useState<string | null>(null);

  const handleGoogleSignIn = async () => {
    if (!googleConfigured || !googleRequest || loadingProvider) return;
    setLoadingProvider('google');
    setAuthError(null);
    try {
      const result = await promptGoogleAsync();
      if (result.type !== 'success') return;

      const idToken = result.params.id_token;
      if (!idToken) throw new Error('Google sign-in did not return an ID token.');

      const authResponse = await signInWithGoogle(idToken);
      await signIn(authResponse);
      router.replace('/explore');
    } catch (error: unknown) {
      setAuthError(error instanceof Error ? error.message : 'Google sign-in failed.');
    } finally {
      setLoadingProvider(null);
    }
  };

  const handleGithubSignIn = async () => {
    if (!githubConfigured || !githubRequest || loadingProvider) return;
    setLoadingProvider('github');
    setAuthError(null);
    console.info('[GitHub OAuth] Starting sign-in', {
      redirectUri: githubRedirectUri,
      clientConfigured: githubConfigured,
    });
    try {
      const result = await promptGithubAsync();
      console.info('[GitHub OAuth] Authorization result', {
        type: result.type,
        hasCodeVerifier: Boolean(githubRequest.codeVerifier),
        error: result.type === 'error' ? result.error : undefined,
      });
      if (result.type !== 'success' || !githubRequest.codeVerifier) return;

      const code = result.params.code;
      if (!code) throw new Error('GitHub sign-in did not return an authorization code.');

      const authResponse = await exchangeGithubCode(code, githubRequest.codeVerifier, githubRedirectUri);
      await signIn(authResponse);
      router.replace('/explore');
    } catch (error: unknown) {
      console.error('[GitHub OAuth] Sign-in failed', error);
      setAuthError(error instanceof Error ? error.message : 'GitHub sign-in failed.');
    } finally {
      setLoadingProvider(null);
    }
  };

  return (
    <View style={styles.page}>
      <Animated.View pointerEvents="none" style={[styles.backgroundPulse, styles.backgroundPulseTop, pulseStyle]} />
      <Animated.View
        pointerEvents="none"
        style={[styles.backgroundPulse, styles.backgroundPulseBottom, pulseStyle, { transform: [{ scale: pulse.interpolate({ inputRange: [0, 1], outputRange: [1.08, 0.92] }) }] }]}
      />
      <View pointerEvents="none" style={styles.waterfallLayer}>
        {waterfallBubbleConfigs.map((bubble, index) => {
          const progress = waterfallProgress[index];
          return (
            <Animated.View
              key={`${bubble.left}-${bubble.size}`}
              style={[
                styles.waterfallBubble,
                {
                  left: bubble.left,
                  width: bubble.size,
                  height: bubble.size,
                  borderRadius: bubble.size / 2,
                  opacity: progress.interpolate({ inputRange: [0, 0.08, 0.8, 1], outputRange: [0, 0.65, 0.4, 0] }),
                  transform: [{ translateY: progress.interpolate({ inputRange: [0, 1], outputRange: [-80, 820] }) }],
                },
              ]}
            />
          );
        })}
      </View>
      <ScrollView contentContainerStyle={styles.scrollContent} keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false}>
        <SafeAreaView style={styles.safeArea}>
          <View style={[styles.content, isWide && styles.contentWide]}>
            <View style={[styles.hero, isWide && styles.heroWide]}>
              <View style={styles.eyebrowRow}><View style={styles.eyebrowDot} /><Text style={styles.eyebrow}>{eyebrowMessage}</Text></View>
              <Text style={styles.title}>Hygiene Tracker</Text>
              <Text style={styles.heroCopy}>Keep your daily hygiene habits simple, consistent, and easy to return to.</Text>

              <View style={styles.ritualCard}>
                <View style={styles.ritualCardTop}>
                  <View><Text style={styles.ritualLabel}>TODAY&apos;S PROGRESS</Text><Text style={styles.ritualTitle}>You&apos;re doing great</Text></View>
                  <View style={styles.streakBadge}><Text style={styles.streakNumber}>07</Text><Text style={styles.streakLabel}>day streak</Text></View>
                </View>
                <View style={styles.progressTrack}><View style={styles.progressFill} /></View>
                <View style={styles.ritualFooter}><Text style={styles.ritualFooterText}>4 of 5 rituals complete</Text><Text style={styles.ritualFooterText}>80%</Text></View>
              </View>

              <View style={styles.quoteRow}><View style={styles.quoteMark}><Text style={styles.quoteMarkText}>“</Text></View><Text style={styles.quote}>Consistency feels better than perfection.</Text></View>
            </View>

            <Animated.View style={[styles.formCard, formEntranceStyle]}>
              <Text style={styles.formKicker}>WELCOME BACK</Text>
              <Text style={styles.formTitle}>Let&apos;s pick up where you left off.</Text>
              <Text style={styles.formDescription}>Sign in to keep your hygiene habits moving.</Text>

              <Animated.View style={githubEntranceStyle}>
                <Pressable
                  accessibilityRole="button"
                  disabled={!githubConfigured || !githubRequest || loadingProvider !== null}
                  onPress={() => void handleGithubSignIn()}
                  onHoverIn={() => {
                    githubHovered.current = true;
                    animateButtonPress(githubScale, 1.03);
                  }}
                  onHoverOut={() => {
                    githubHovered.current = false;
                    animateButtonPress(githubScale, 1);
                  }}
                  onPressIn={() => animateButtonPress(githubScale, 0.97)}
                  onPressOut={() => animateButtonPress(githubScale, githubHovered.current ? 1.03 : 1)}
                  style={({ pressed }) => [
                    styles.githubButton,
                    (!githubConfigured || !githubRequest || loadingProvider !== null) && styles.disabledButton,
                    pressed && styles.buttonPressed,
                  ]}>
                  {loadingProvider === 'github' ? <ActivityIndicator color="#222222" /> : <>
                    <Image source={require('../../assets/images/github_login_icon.png')} style={styles.githubIcon} />
                    <Text numberOfLines={1} style={styles.githubButtonText}>Sign in with GitHub</Text>
                  </>}
                </Pressable>
              </Animated.View>
              <Animated.View style={googleEntranceStyle}>
                <Pressable
                  accessibilityRole="button"
                  disabled={!googleConfigured || !googleRequest || loadingProvider !== null}
                  onPress={() => void handleGoogleSignIn()}
                  onHoverIn={() => {
                    googleHovered.current = true;
                    animateButtonPress(googleScale, 1.03);
                  }}
                  onHoverOut={() => {
                    googleHovered.current = false;
                    animateButtonPress(googleScale, 1);
                  }}
                  onPressIn={() => animateButtonPress(googleScale, 0.97)}
                  onPressOut={() => animateButtonPress(googleScale, googleHovered.current ? 1.03 : 1)}
                  style={({ pressed }) => [
                    styles.googleButton,
                    (!googleConfigured || !googleRequest || loadingProvider !== null) && styles.disabledButton,
                    pressed && styles.buttonPressed,
                  ]}>
                  {loadingProvider === 'google' ? <ActivityIndicator color="#222222" /> :
                    <Image source={require('../../assets/images/google_login_button.png')} style={styles.googleButtonImage} resizeMode="contain" />}
                </Pressable>
              </Animated.View>
              {!googleConfigured || !githubConfigured || authError ? (
                <Text style={styles.authMessage}>
                  {authError || 'Add the OAuth client IDs to .env, then restart Expo.'}
                </Text>
              ) : null}
            </Animated.View>
          </View>

          <View style={styles.footer}><Text style={styles.footerText}>© 2026 Hygiene Tracker</Text><View style={styles.footerLinks}><Text style={styles.footerText}>Jaime Serrano, Leonardo Lopez, Hugo Ruiz-Mireles</Text><View style={styles.footerDot} /><Text style={styles.footerText}>CSUMB School of Computing & Desing</Text></View></View>
        </SafeAreaView>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: colors.background }, scrollContent: { flexGrow: 1 },
  backgroundPulse: { position: 'absolute', width: 360, height: 360, borderRadius: 180, backgroundColor: '#78A9E8' },
  backgroundPulseTop: { top: -150, right: -100 }, backgroundPulseBottom: { bottom: -180, left: -120 },
  waterfallLayer: { ...StyleSheet.absoluteFill, overflow: 'hidden' },
  waterfallBubble: { position: 'absolute', top: 0, backgroundColor: '#5F9BE4', shadowColor: '#FFFFFF', shadowOpacity: 0.35, shadowRadius: 4, elevation: 2 },
  safeArea: { flex: 1, width: '100%', maxWidth: 1280, alignSelf: 'center', paddingHorizontal: 28 },
  content: { width: '100%', alignItems: 'center', paddingTop: 48, paddingBottom: 36, gap: 40 }, contentWide: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 100, paddingTop: 64, paddingBottom: 78 }, hero: { width: '100%', maxWidth: 520 }, heroWide: { flex: 1, maxWidth: 500 },
  eyebrowRow: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 20 }, eyebrowDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: colors.blue }, eyebrow: { color: colors.blue, fontSize: 12, fontWeight: '800', letterSpacing: 1.4 }, title: { color: colors.ink, fontSize: 58, lineHeight: 61, fontWeight: '800', letterSpacing: -2.5 }, heroCopy: { color: colors.muted, fontSize: 17, lineHeight: 26, maxWidth: 420, marginTop: 22 },
  ritualCard: { backgroundColor: colors.paleBlue, borderRadius: 22, padding: 22, marginTop: 38, maxWidth: 430 }, ritualCardTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' }, ritualLabel: { color: colors.blue, fontSize: 10, fontWeight: '800', letterSpacing: 1.4, marginBottom: 7 }, ritualTitle: { color: colors.ink, fontSize: 20, fontWeight: '700' }, streakBadge: { alignItems: 'flex-end' }, streakNumber: { color: colors.blue, fontSize: 23, fontWeight: '800' }, streakLabel: { color: colors.muted, fontSize: 11, marginTop: -2 }, progressTrack: { height: 9, backgroundColor: colors.sky, borderRadius: 6, marginTop: 22, overflow: 'hidden' }, progressFill: { width: '80%', height: '100%', backgroundColor: colors.blue, borderRadius: 6 }, ritualFooter: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 10 }, ritualFooterText: { color: colors.muted, fontSize: 12, fontWeight: '600' },
  quoteRow: { flexDirection: 'row', alignItems: 'center', gap: 12, marginTop: 28 }, quoteMark: { width: 34, height: 34, borderRadius: 17, backgroundColor: colors.sky, alignItems: 'center', justifyContent: 'center' }, quoteMarkText: { color: colors.blue, fontSize: 25, lineHeight: 30, fontWeight: '700' }, quote: { color: colors.muted, fontSize: 13, fontStyle: 'italic' },
  formCard: { width: '100%', maxWidth: 440, backgroundColor: colors.surface, borderRadius: 26, padding: 28, shadowColor: colors.ink, shadowOpacity: 0.08, shadowRadius: 25, shadowOffset: { width: 0, height: 12 }, elevation: 3 }, formKicker: { color: colors.blue, fontSize: 11, fontWeight: '800', letterSpacing: 1.5, marginBottom: 12 }, formTitle: { color: colors.ink, fontSize: 27, lineHeight: 32, fontWeight: '700', letterSpacing: -0.8 }, formDescription: { color: colors.muted, fontSize: 14, lineHeight: 21, marginTop: 10 },
  githubButton: { width: '100%', maxWidth: 228, height: 51, backgroundColor: colors.surface, borderWidth: 1, borderColor: '#808080', borderRadius: 26, marginTop: 28, paddingHorizontal: 16, alignSelf: 'center', flexDirection: 'row', alignItems: 'center', justifyContent: 'flex-start', gap: 14 }, githubIcon: { width: 33, height: 33 }, githubButtonText: { color: '#222222', fontSize: 14, fontWeight: '600', flexShrink: 1 }, googleButton: { width: '100%', maxWidth: 228, height: 51, marginTop: 12, alignSelf: 'center', alignItems: 'center', justifyContent: 'center' }, googleButtonImage: { width: '100%', height: 51 }, authMessage: { color: '#B42318', fontSize: 12, lineHeight: 18, textAlign: 'center', marginTop: 16 }, disabledButton: { opacity: 0.55 }, buttonPressed: { opacity: 0.78 },
  footer: { width: '100%', flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 20, borderTopWidth: 1, borderTopColor: colors.line }, footerText: { color: colors.muted, fontSize: 11 }, footerLinks: { flexDirection: 'row', alignItems: 'center', gap: 12 }, footerDot: { width: 3, height: 3, borderRadius: 2, backgroundColor: colors.sky },
});
