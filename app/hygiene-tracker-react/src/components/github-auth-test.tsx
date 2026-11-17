import * as WebBrowser from 'expo-web-browser';
import { useAuthRequest } from 'expo-auth-session';
import { useState } from 'react';
import { ActivityIndicator, Platform, Pressable, StyleSheet } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { exchangeGithubCode, githubRedirectUri as redirectUri } from '@/services/github-auth';
import type { AuthResponse } from '@/services/google-auth';

WebBrowser.maybeCompleteAuthSession();

const githubClientId = process.env.EXPO_PUBLIC_GITHUB_CLIENT_ID || 'missing-github-client-id';
const githubDiscovery = {
  authorizationEndpoint: 'https://github.com/login/oauth/authorize',
};

export function GithubAuthTest() {
  const [request, , promptAsync] = useAuthRequest(
    {
      clientId: githubClientId,
      scopes: ['read:user', 'user:email'],
      redirectUri,
      usePKCE: true,
    },
    githubDiscovery
  );
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('Not connected');
  const [auth, setAuth] = useState<AuthResponse | null>(null);
  const supported = Platform.OS !== 'web';

  const handlePress = async () => {
    setLoading(true);
    setAuth(null);
    setMessage('Opening GitHub sign-in…');
    try {
      const result = await promptAsync();
      if (result.type === 'cancel' || result.type === 'dismiss') {
        setMessage('GitHub sign-in was canceled.');
        return;
      }
      if (result.type === 'error') {
        setMessage(result.error?.message || 'GitHub sign-in failed.');
        return;
      }
      if (result.type !== 'success' || !request?.codeVerifier) {
        setMessage('GitHub sign-in did not return a usable authorization code.');
        return;
      }

      const code = result.params.code;
      if (!code) {
        setMessage('GitHub sign-in completed without an authorization code.');
        return;
      }

      const verifiedUser = await exchangeGithubCode(code, request.codeVerifier, redirectUri);
      setAuth(verifiedUser);
      setMessage('GitHub account validated by Spring Boot.');
    } catch (error: unknown) {
      setMessage(error instanceof Error ? error.message : 'Could not validate the GitHub account.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <ThemedView style={styles.container}>
      <Pressable
        accessibilityRole="button"
        disabled={!supported || !request || loading}
        onPress={() => void handlePress()}
        style={({ pressed }) => [
          styles.button,
          (!supported || !request || loading) && styles.disabledButton,
          pressed && styles.pressed,
        ]}>
        {loading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <ThemedText style={styles.buttonText}>Continue with GitHub</ThemedText>
        )}
      </Pressable>
      <ThemedText type="small" accessibilityLiveRegion="polite">
        {supported ? message : 'GitHub PKCE sign-in is currently configured for iOS and Android.'}
      </ThemedText>
      {auth ? (
        <ThemedText type="small">
          Signed-in account: {auth.user.email}
        </ThemedText>
      ) : null}
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
    backgroundColor: '#24292f',
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
