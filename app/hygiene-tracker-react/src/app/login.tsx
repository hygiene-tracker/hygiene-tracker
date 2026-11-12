import * as Google from 'expo-auth-session/providers/google';
import * as WebBrowser from 'expo-web-browser';
import { makeRedirectUri, useAuthRequest } from 'expo-auth-session';
import { router } from 'expo-router';
import { useState } from 'react';
import {
  ActivityIndicator,
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
import { exchangeGithubCode } from '@/services/github-auth';
import { signInWithGoogle } from '@/services/google-auth';

WebBrowser.maybeCompleteAuthSession();

const webClientId = process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID;
const androidClientId = process.env.EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID;
const iosClientId = process.env.EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID;
const githubClientId = process.env.EXPO_PUBLIC_GITHUB_CLIENT_ID;
const googleConfigured = Boolean(webClientId || androidClientId || iosClientId);
const githubConfigured = Boolean(githubClientId);
const githubRedirectUri = makeRedirectUri({
  scheme: 'hygiene-tracker',
  path: 'auth/github',
});
const githubDiscovery = {
  authorizationEndpoint: 'https://github.com/login/oauth/authorize',
};

const colors = {
  ink: '#102A43', muted: '#5B7083', background: '#F4F8FF', surface: '#FFFFFF',
  paleBlue: '#EAF2FF', blue: '#2563EB', line: '#D7E4F3', sky: '#BBD7FF',
};

export default function LoginScreen() {
  const { signIn } = useAuth();
  const { width } = useWindowDimensions();
  const isWide = width >= 840;
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
    try {
      const result = await promptGithubAsync();
      if (result.type !== 'success' || !githubRequest.codeVerifier) return;

      const code = result.params.code;
      if (!code) throw new Error('GitHub sign-in did not return an authorization code.');

      const authResponse = await exchangeGithubCode(code, githubRequest.codeVerifier, githubRedirectUri);
      await signIn(authResponse);
      router.replace('/explore');
    } catch (error: unknown) {
      setAuthError(error instanceof Error ? error.message : 'GitHub sign-in failed.');
    } finally {
      setLoadingProvider(null);
    }
  };

  return (
    <View style={styles.page}>
      <ScrollView contentContainerStyle={styles.scrollContent} keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false}>
        <SafeAreaView style={styles.safeArea}>
          <View style={[styles.content, isWide && styles.contentWide]}>
            <View style={[styles.hero, isWide && styles.heroWide]}>
              <View style={styles.eyebrowRow}><View style={styles.eyebrowDot} /><Text style={styles.eyebrow}>Time to shower, you punk rocker</Text></View>
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

            <View style={styles.formCard}>
              <Text style={styles.formKicker}>WELCOME BACK</Text>
              <Text style={styles.formTitle}>Let&apos;s pick up where you left off.</Text>
              <Text style={styles.formDescription}>Sign in to keep your hygiene habits moving.</Text>

              <Pressable
                accessibilityRole="button"
                disabled={!githubConfigured || !githubRequest || loadingProvider !== null}
                onPress={() => void handleGithubSignIn()}
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
              <Pressable
                accessibilityRole="button"
                disabled={!googleConfigured || !googleRequest || loadingProvider !== null}
                onPress={() => void handleGoogleSignIn()}
                style={({ pressed }) => [
                  styles.googleButton,
                  (!googleConfigured || !googleRequest || loadingProvider !== null) && styles.disabledButton,
                  pressed && styles.buttonPressed,
                ]}>
                {loadingProvider === 'google' ? <ActivityIndicator color="#222222" /> :
                  <Image source={require('../../assets/images/google_login_button.png')} style={styles.googleButtonImage} resizeMode="contain" />}
              </Pressable>
              {!googleConfigured || !githubConfigured || authError ? (
                <Text style={styles.authMessage}>
                  {authError || 'Add the OAuth client IDs to .env, then restart Expo.'}
                </Text>
              ) : null}
            </View>
          </View>

          <View style={styles.footer}><Text style={styles.footerText}>© 2025 good habit</Text><View style={styles.footerLinks}><Text style={styles.footerText}>Made for your everyday</Text><View style={styles.footerDot} /><Text style={styles.footerText}>Support</Text></View></View>
        </SafeAreaView>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: colors.background }, scrollContent: { flexGrow: 1 },
  safeArea: { flex: 1, width: '100%', maxWidth: 1280, alignSelf: 'center', paddingHorizontal: 28 },
  content: { width: '100%', alignItems: 'center', paddingTop: 48, paddingBottom: 36, gap: 40 }, contentWide: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 100, paddingTop: 64, paddingBottom: 78 }, hero: { width: '100%', maxWidth: 520 }, heroWide: { flex: 1, maxWidth: 500 },
  eyebrowRow: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 20 }, eyebrowDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: colors.blue }, eyebrow: { color: colors.blue, fontSize: 12, fontWeight: '800', letterSpacing: 1.4 }, title: { color: colors.ink, fontSize: 58, lineHeight: 61, fontWeight: '800', letterSpacing: -2.5 }, heroCopy: { color: colors.muted, fontSize: 17, lineHeight: 26, maxWidth: 420, marginTop: 22 },
  ritualCard: { backgroundColor: colors.paleBlue, borderRadius: 22, padding: 22, marginTop: 38, maxWidth: 430 }, ritualCardTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' }, ritualLabel: { color: colors.blue, fontSize: 10, fontWeight: '800', letterSpacing: 1.4, marginBottom: 7 }, ritualTitle: { color: colors.ink, fontSize: 20, fontWeight: '700' }, streakBadge: { alignItems: 'flex-end' }, streakNumber: { color: colors.blue, fontSize: 23, fontWeight: '800' }, streakLabel: { color: colors.muted, fontSize: 11, marginTop: -2 }, progressTrack: { height: 9, backgroundColor: colors.sky, borderRadius: 6, marginTop: 22, overflow: 'hidden' }, progressFill: { width: '80%', height: '100%', backgroundColor: colors.blue, borderRadius: 6 }, ritualFooter: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 10 }, ritualFooterText: { color: colors.muted, fontSize: 12, fontWeight: '600' },
  quoteRow: { flexDirection: 'row', alignItems: 'center', gap: 12, marginTop: 28 }, quoteMark: { width: 34, height: 34, borderRadius: 17, backgroundColor: colors.sky, alignItems: 'center', justifyContent: 'center' }, quoteMarkText: { color: colors.blue, fontSize: 25, lineHeight: 30, fontWeight: '700' }, quote: { color: colors.muted, fontSize: 13, fontStyle: 'italic' },
  formCard: { width: '100%', maxWidth: 440, backgroundColor: colors.surface, borderRadius: 26, padding: 28, shadowColor: colors.ink, shadowOpacity: 0.08, shadowRadius: 25, shadowOffset: { width: 0, height: 12 }, elevation: 3 }, formKicker: { color: colors.blue, fontSize: 11, fontWeight: '800', letterSpacing: 1.5, marginBottom: 12 }, formTitle: { color: colors.ink, fontSize: 27, lineHeight: 32, fontWeight: '700', letterSpacing: -0.8 }, formDescription: { color: colors.muted, fontSize: 14, lineHeight: 21, marginTop: 10 },
  githubButton: { width: '100%', maxWidth: 228, height: 51, backgroundColor: colors.surface, borderWidth: 1, borderColor: '#808080', borderRadius: 26, marginTop: 28, paddingHorizontal: 16, alignSelf: 'center', flexDirection: 'row', alignItems: 'center', justifyContent: 'flex-start', gap: 14 }, githubIcon: { width: 33, height: 33 }, githubButtonText: { color: '#222222', fontSize: 14, fontWeight: '600', flexShrink: 1 }, googleButton: { width: '100%', maxWidth: 228, height: 51, marginTop: 12, alignSelf: 'center', alignItems: 'center', justifyContent: 'center' }, googleButtonImage: { width: '100%', height: 51 }, authMessage: { color: '#B42318', fontSize: 12, lineHeight: 18, textAlign: 'center', marginTop: 16 }, disabledButton: { opacity: 0.55 }, buttonPressed: { opacity: 0.78 },
  footer: { width: '100%', flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 20, borderTopWidth: 1, borderTopColor: colors.line }, footerText: { color: colors.muted, fontSize: 11 }, footerLinks: { flexDirection: 'row', alignItems: 'center', gap: 12 }, footerDot: { width: 3, height: 3, borderRadius: 2, backgroundColor: colors.sky },
});
