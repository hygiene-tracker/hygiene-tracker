import { GoogleSignin } from '@react-native-google-signin/google-signin';
import { Platform } from 'react-native';

const webClientId = process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID?.trim();

export function configureGoogleAuth() {
  if (Platform.OS === 'web') {
    return;
  }

  if (!webClientId) {
    console.warn(
      '[GoogleAuth] EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID is not set. Add the Google OAuth web client ID before using Google sign-in.',
    );
    GoogleSignin.configure({
      scopes: ['profile', 'email'],
    });
    return;
  }

  GoogleSignin.configure({
    webClientId,
    offlineAccess: true,
    scopes: ['profile', 'email'],
  });
}

export { GoogleSignin };
