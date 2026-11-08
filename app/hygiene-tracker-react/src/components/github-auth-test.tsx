import * as WebBrowser from 'expo-web-browser';
import { useState } from 'react';
import { ActivityIndicator, Platform, Pressable, StyleSheet } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';
import { signInWithGithub, type VerifiedGithubUser } from '@/services/github-auth';

WebBrowser.maybeCompleteAuthSession();

export function GithubAuthTest() {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('Not connected');
  const [user, setUser] = useState<VerifiedGithubUser | null>(null);
  const supported = Platform.OS !== 'web';

  const handlePress = async () => {
    setLoading(true);
    setUser(null);
    setMessage('Opening GitHub sign-in…');
    try {
      const verifiedUser = await signInWithGithub();
      setUser(verifiedUser);
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
        disabled={!supported || loading}
        onPress={() => void handlePress()}
        style={({ pressed }) => [
          styles.button,
          (!supported || loading) && styles.disabledButton,
          pressed && styles.pressed,
        ]}>
        {loading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <ThemedText style={styles.buttonText}>Continue with GitHub</ThemedText>
        )}
      </Pressable>
      <ThemedText type="small" accessibilityLiveRegion="polite">
        {supported ? message : 'GitHub app handoff is currently configured for iOS and Android.'}
      </ThemedText>
      {user ? (
        <ThemedText type="small">
          Verified account: {user.email ?? `@${user.login}`}
          {user.email && !user.emailVerified ? ' (unverified)' : ''}
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
