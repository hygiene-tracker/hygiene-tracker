import * as Google from 'expo-auth-session/providers/google';
import * as WebBrowser from 'expo-web-browser';
import { useState } from 'react';
import { ActivityIndicator, Platform, Pressable, StyleSheet } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { validateGoogleIdToken, type VerifiedGoogleUser } from '@/services/google-auth';

WebBrowser.maybeCompleteAuthSession();

const webClientId = process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID;
const androidClientId = process.env.EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID;
const iosClientId = process.env.EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID;

function hasPlatformClientId() {
  if (Platform.OS === 'android') return Boolean(androidClientId);
  if (Platform.OS === 'ios') return Boolean(iosClientId);
  return Boolean(webClientId);
}

export function GoogleAuthTest() {
  const [request, , promptAsync] = Google.useIdTokenAuthRequest({
    androidClientId: androidClientId || webClientId || 'missing.apps.googleusercontent.com',
    iosClientId: iosClientId || webClientId || 'missing.apps.googleusercontent.com',
    webClientId: webClientId || 'missing.apps.googleusercontent.com',
    scopes: ['openid', 'email', 'profile'],
    selectAccount: true,
  });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('Not connected');
  const [user, setUser] = useState<VerifiedGoogleUser | null>(null);

  const configured = hasPlatformClientId();

  const handlePress = async () => {
    setLoading(true);
    setUser(null);
    setMessage('Opening Google sign-in…');

    try {
      const result = await promptAsync();
      if (result.type === 'cancel' || result.type === 'dismiss') {
        setMessage('Google sign-in was canceled.');
        return;
      }
      if (result.type === 'error') {
        setMessage(result.error?.message || 'Google sign-in failed.');
        return;
      }
      if (result.type !== 'success') {
        setMessage('Google sign-in did not complete.');
        return;
      }

      const idToken = result.params.id_token;
      if (!idToken) {
        setMessage('Google sign-in completed, but no ID token was returned.');
        return;
      }

      setMessage('Checking token with Spring Boot…');
      const verifiedUser = await validateGoogleIdToken(idToken);
      setUser(verifiedUser);
      setMessage('Google token verified by Spring Boot.');
    } catch (error: unknown) {
      setMessage(error instanceof Error ? error.message : 'Could not validate the Google token.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <ThemedView style={styles.container}>
      <Pressable
        accessibilityRole="button"
        disabled={!request || !configured || loading}
        onPress={() => void handlePress()}
        style={({ pressed }) => [
          styles.button,
          (!request || !configured || loading) && styles.disabledButton,
          pressed && styles.pressed,
        ]}>
        {loading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <ThemedText style={styles.buttonText}>Continue with Google</ThemedText>
        )}
      </Pressable>
      <ThemedText type="small" accessibilityLiveRegion="polite">
        {configured ? message : `Add a Google ${Platform.OS} client ID to the app environment.`}
      </ThemedText>
      {user?.email ? <ThemedText type="small">Verified account: {user.email}</ThemedText> : null}
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: Spacing.two,
    alignSelf: 'stretch',
  },
  button: {
    minHeight: 48,
    paddingHorizontal: Spacing.four,
    borderRadius: Spacing.three,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#1a73e8',
  },
  buttonText: {
    color: '#fff',
  },
  disabledButton: {
    opacity: 0.55,
  },
  pressed: {
    opacity: 0.8,
  },
});
