import { useState } from 'react';
import { Image } from 'expo-image';
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { AppShell } from '@/components/app-shell';
import { useAuth } from '@/context/auth-context';
import { AVATAR_OPTIONS } from '@/lib/avatar-catalog';
import { apiFetch } from '@/services/api-client';
import type { AuthUser } from '@/services/auth';

const colors = {
  background: '#E8F1FF',
  surface: '#FFFFFF',
  ink: '#102A43',
  muted: '#5B7083',
  blue: '#2563EB',
  line: '#C8D9EF',
  disabled: '#F1F5F9',
};

export default function ProfileScreen() {
  const { user, updateUser } = useAuth();
  const [nickname, setNickname] = useState(user?.displayName ?? '');
  const [avatar, setAvatar] = useState(user?.avatar ?? '');
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSave = async () => {
    const trimmedNickname = nickname.trim();
    if (!trimmedNickname || saving) return;

    setSaving(true);
    setSaved(false);
    setError(null);
    try {
      const response = await apiFetch('/api/me', {
        method: 'PATCH',
        body: JSON.stringify({ displayName: trimmedNickname, ...(avatar ? { avatar } : {}) }),
      });
      if (!response.ok) {
        const problem = await response.json().catch(() => null) as { detail?: string } | null;
        throw new Error(problem?.detail || `Unable to save your nickname (HTTP ${response.status}).`);
      }
      await updateUser(await response.json() as AuthUser);
      setSaved(true);
    } catch (saveError: unknown) {
      setError(saveError instanceof Error ? saveError.message : 'Unable to save your nickname.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppShell>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.page}>
        <View style={styles.card}>
          <Text style={styles.eyebrow}>PROFILE SETTINGS</Text>
          <Text style={styles.title}>Your profile</Text>
          <Text style={styles.description}>Update how you appear in Hygiene Tracker.</Text>

          <View style={styles.fieldGroup}>
            <Text style={styles.label}>Nickname</Text>
            <TextInput
              accessibilityLabel="Nickname"
              autoCapitalize="words"
              autoCorrect={false}
              maxLength={32}
              onChangeText={(value) => {
                setNickname(value);
                setSaved(false);
              }}
              placeholder="e.g. Fresh Start"
              placeholderTextColor="#8AA0B5"
              style={styles.input}
              value={nickname}
            />
          </View>

          {AVATAR_OPTIONS.length > 0 ? <View style={styles.fieldGroup}>
            <View style={styles.labelRow}>
              <Text style={styles.label}>Avatar</Text>
              <Text style={styles.comingSoon}>CHOOSE ONE</Text>
            </View>
            <View style={styles.avatarOptions}>
              {AVATAR_OPTIONS.map((option) => (
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel={`Choose avatar ${option.key}`}
                  key={option.key}
                  onPress={() => {
                    setAvatar(option.key);
                    setSaved(false);
                  }}
                  style={[styles.avatarOption, avatar === option.key && styles.selectedAvatarOption]}>
                  <Image source={option.source} style={styles.avatarOptionImage} />
                </Pressable>
              ))}
            </View>
          </View> : null}

          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Save nickname"
            disabled={!nickname.trim() || saving}
            onPress={() => void handleSave()}
            style={({ pressed }) => [styles.saveButton, (!nickname.trim() || saving) && styles.disabledButton, pressed && styles.pressed]}>
            <Text style={styles.saveButtonText}>{saving ? 'Saving…' : 'Save nickname'}</Text>
          </Pressable>
          {saved ? <Text style={styles.successText}>Profile updated.</Text> : null}
          {error ? <Text accessibilityLiveRegion="polite" style={styles.errorText}>{error}</Text> : null}
        </View>
      </KeyboardAvoidingView>
    </AppShell>
  );
}

const styles = StyleSheet.create({
  page: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background, padding: 24 },
  card: { width: '100%', maxWidth: 440, backgroundColor: colors.surface, borderRadius: 26, padding: 28, shadowColor: colors.ink, shadowOpacity: 0.08, shadowRadius: 25, shadowOffset: { width: 0, height: 12 }, elevation: 3 },
  eyebrow: { color: colors.blue, fontSize: 11, fontWeight: '800', letterSpacing: 1.5, marginBottom: 10 },
  title: { color: colors.ink, fontSize: 30, lineHeight: 36, fontWeight: '800', letterSpacing: -0.8 },
  description: { color: colors.muted, fontSize: 15, lineHeight: 22, marginTop: 10, marginBottom: 26 },
  fieldGroup: { marginBottom: 22 },
  labelRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  label: { color: colors.ink, fontSize: 13, fontWeight: '700', marginBottom: 8 },
  comingSoon: { color: colors.muted, fontSize: 10, fontWeight: '800', letterSpacing: 0.8, marginBottom: 8 },
  input: { height: 50, borderWidth: 1, borderColor: colors.line, borderRadius: 12, paddingHorizontal: 15, color: colors.ink, fontSize: 15, backgroundColor: '#FBFDFF' },
  avatarButton: { height: 72, borderWidth: 1, borderColor: colors.line, borderStyle: 'dashed', borderRadius: 12, backgroundColor: colors.disabled, alignItems: 'center', justifyContent: 'center', flexDirection: 'row', gap: 8, opacity: 0.72 },
  avatarIcon: { color: colors.muted, fontSize: 24, fontWeight: '300' },
  avatarButtonText: { color: colors.muted, fontSize: 14, fontWeight: '700' },
  helperText: { color: colors.muted, fontSize: 11, marginTop: 8 },
  avatarOptions: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  avatarOption: { width: 58, height: 58, borderRadius: 29, borderWidth: 2, borderColor: colors.line, alignItems: 'center', justifyContent: 'center' },
  selectedAvatarOption: { borderColor: colors.blue, borderWidth: 3 },
  avatarOptionImage: { width: 50, height: 50, borderRadius: 25 },
  saveButton: { height: 50, borderRadius: 25, backgroundColor: colors.blue, alignItems: 'center', justifyContent: 'center', marginTop: 2 },
  saveButtonText: { color: colors.surface, fontSize: 15, fontWeight: '800' },
  disabledButton: { opacity: 0.55 },
  successText: { color: '#147D64', fontSize: 12, textAlign: 'center', marginTop: 12 },
  errorText: { color: '#B42318', fontSize: 12, lineHeight: 18, textAlign: 'center', marginTop: 12 },
  pressed: { opacity: 0.82 },
});
