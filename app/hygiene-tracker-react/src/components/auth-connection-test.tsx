import { useState } from 'react';
import { ActivityIndicator, Platform, Pressable, StyleSheet } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { Spacing } from '@/constants/theme';

const defaultApiHost = Platform.OS === 'android' ? '10.0.2.2' : 'localhost';
const apiBaseUrl = (process.env.EXPO_PUBLIC_API_BASE_URL || `http://${defaultApiHost}:8080`).replace(
  /\/$/,
  ''
);

type MockResponse = {
  valid: boolean;
  provider: string;
  message: string;
  user: {
    id: string;
    login: string;
    name: string;
    email: string;
  };
};

export function AuthConnectionTest() {
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState<number | null>(null);
  const [message, setMessage] = useState('Not tested');
  const [user, setUser] = useState<MockResponse['user'] | null>(null);

  const runTest = async () => {
    setLoading(true);
    setStatus(null);
    setUser(null);
    setMessage('Contacting Spring Boot…');
    try {
      const response = await fetch(`${apiBaseUrl}/api/auth/connection-test`);
      setStatus(response.status);
      const body = (await response.json().catch(() => null)) as MockResponse | null;
      if (!response.ok) {
        setMessage(body?.message || `Spring Boot returned HTTP ${response.status}.`);
        return;
      }
      if (!body?.valid || !body.user) {
        setMessage('Spring Boot responded, but the mock response was incomplete.');
        return;
      }
      setUser(body.user);
      setMessage(body.message);
    } catch (error: unknown) {
      setMessage(error instanceof Error ? error.message : 'Could not reach Spring Boot.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <ThemedView style={styles.container}>
      <Pressable
        accessibilityRole="button"
        disabled={loading}
        onPress={() => void runTest()}
        style={({ pressed }) => [styles.button, loading && styles.disabledButton, pressed && styles.pressed]}>
        {loading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <ThemedText style={styles.buttonText}>Test Spring Boot Connection</ThemedText>
        )}
      </Pressable>
      <ThemedText type="small" accessibilityLiveRegion="polite">
        {status === null ? message : `HTTP ${status}: ${message}`}
      </ThemedText>
      {user ? (
        <ThemedText type="small">
          Mock user: {user.name} (@{user.login}) · {user.email}
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
    backgroundColor: '#52606d',
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
