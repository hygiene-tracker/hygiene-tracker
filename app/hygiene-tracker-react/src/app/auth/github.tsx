import * as WebBrowser from 'expo-web-browser';
import { StyleSheet, Text, View } from 'react-native';

WebBrowser.maybeCompleteAuthSession();

export default function GithubAuthCallback() {
  return (
    <View style={styles.container}>
      <Text style={styles.text}>Completing GitHub sign-in…</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#F4F8FF',
  },
  text: {
    color: '#102A43',
    fontSize: 16,
  },
});
