import type { ImageSourcePropType } from 'react-native';

/**
 * Add bundled avatars here as they are added to assets/images/avatars.
 * Example:
 * { key: 'blue.png', source: require('@/assets/images/avatars/blue.png') }
 */
export const AVATAR_OPTIONS: readonly {
  key: string;
  source: ImageSourcePropType;
}[] = [];

export function avatarSource(key: string | null | undefined): ImageSourcePropType | null {
  if (!key) return null;
  return AVATAR_OPTIONS.find((avatar) => avatar.key === key)?.source ?? null;
}
