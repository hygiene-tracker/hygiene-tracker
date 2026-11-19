import { StyleSheet, Text, View } from 'react-native';

import { TopNav } from '@/components/top-nav';

const colors = {
  ink: '#102A43',
  muted: '#5B7083',
  background: '#E8F1FF',
  surface: '#FFFFFF',
  paleBlue: '#EAF2FF',
  blue: '#2563EB',
  line: '#C8D9EF',
  sky: '#A4C7F5',
};

export default function LeaderboardScreen() {
  return (
    <View style={styles.page}>
      <View pointerEvents="none" style={[styles.backgroundOrb, styles.backgroundOrbTop]} />
      <View pointerEvents="none" style={[styles.backgroundOrb, styles.backgroundOrbBottom]} />
      <TopNav />
      <View style={styles.content}>
        <View style={styles.eyebrowRow}>
          <View style={styles.eyebrowDot} />
          <Text style={styles.eyebrow}>MONTHLY LEADERBOARD</Text>
        </View>
        <Text style={styles.title}>Leaderboard</Text>
        <Text style={styles.copy}>A simple look at who needs a little extra motivation this month.</Text>

        <View style={styles.card}>
          <Text style={styles.cardKicker}>LOSER OF THE MONTH</Text>
          <View style={styles.userRow}>
            <View style={styles.userAvatar}>
              <Text style={styles.userAvatarText}>?</Text>
            </View>
            <View style={styles.userCopy}>
              <Text style={styles.userName}>User not available</Text>
              <Text style={styles.userStatus}>Users are not available for this feature yet.</Text>
            </View>
          </View>
        </View>

        <View style={styles.noteRow}>
          <View style={styles.noteMark}><Text style={styles.noteMarkText}>i</Text></View>
          <Text style={styles.note}>The status table will power this feature once it is connected.</Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: colors.background },
  backgroundOrb: { position: 'absolute', width: 300, height: 300, borderRadius: 150, backgroundColor: '#78A9E8', opacity: 0.2 },
  backgroundOrbTop: { top: -150, right: -100 },
  backgroundOrbBottom: { bottom: -180, left: -120 },
  content: { width: '100%', maxWidth: 520, alignSelf: 'center', paddingHorizontal: 28, paddingTop: 56, paddingBottom: 48 },
  eyebrowRow: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 16 },
  eyebrowDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: colors.blue },
  eyebrow: { color: colors.blue, fontSize: 11, fontWeight: '800', letterSpacing: 1.5 },
  title: { color: colors.ink, fontSize: 48, lineHeight: 54, fontWeight: '800', letterSpacing: -2 },
  copy: { color: colors.muted, fontSize: 16, lineHeight: 24, marginTop: 16, maxWidth: 430 },
  card: { backgroundColor: colors.surface, borderRadius: 26, padding: 28, marginTop: 36, shadowColor: colors.ink, shadowOpacity: 0.08, shadowRadius: 25, shadowOffset: { width: 0, height: 12 }, elevation: 3 },
  cardKicker: { color: colors.blue, fontSize: 11, fontWeight: '800', letterSpacing: 1.5, marginBottom: 24 },
  userRow: { flexDirection: 'row', alignItems: 'center', gap: 16 },
  userAvatar: { width: 56, height: 56, borderRadius: 28, backgroundColor: colors.paleBlue, alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: colors.line },
  userAvatarText: { color: colors.blue, fontSize: 24, fontWeight: '800' },
  userCopy: { flex: 1 },
  userName: { color: colors.ink, fontSize: 22, fontWeight: '700' },
  userStatus: { color: colors.muted, fontSize: 13, lineHeight: 19, marginTop: 4 },
  noteRow: { flexDirection: 'row', alignItems: 'center', gap: 12, marginTop: 28 },
  noteMark: { width: 30, height: 30, borderRadius: 15, backgroundColor: colors.sky, alignItems: 'center', justifyContent: 'center' },
  noteMarkText: { color: colors.blue, fontSize: 17, fontWeight: '800' },
  note: { color: colors.muted, fontSize: 12, lineHeight: 18, flex: 1 },
});
