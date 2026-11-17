import * as WebBrowser from 'expo-web-browser';
import { StyleSheet } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';

WebBrowser.maybeCompleteAuthSession();

export default function AuthRedirectScreen() {
  return (
    <ThemedView style={styles.container}>
      <ThemedText>Signing you in…</ThemedText>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
