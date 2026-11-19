import { StyleSheet, View } from 'react-native';

import { TopNav } from '@/components/top-nav';

export function HomeScreen() {
  return (
    <View style={styles.container}>
      <TopNav />
      <View style={styles.blank} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  blank: {
    flex: 1,
  },
});
