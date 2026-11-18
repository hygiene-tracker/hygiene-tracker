import { StyleSheet, View } from 'react-native';

import { AppShell } from '@/components/app-shell';

export default function Index() {
  return (
    <AppShell>
      <View style={styles.container} />
    </AppShell>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FFFFFF' },
});
