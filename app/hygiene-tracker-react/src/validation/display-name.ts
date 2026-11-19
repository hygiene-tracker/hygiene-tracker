/** Longest display name the API accepts (`UpdateMeRequest.displayName` in `docs/openapi.yaml`). */
export const DISPLAY_NAME_MAX_LENGTH = 32;

/**
 * Checks a display name before it is sent to `PATCH /api/me` (PRD-2), using the same rules as
 * the API: 1 to 32 characters after trimming spaces from both ends.
 *
 * @param displayName what the user typed
 * @returns the error message to show, or `null` if the display name is valid
 */
export function validateDisplayName(displayName: string): string | null {
  const trimmed = displayName.trim();
  if (trimmed.length === 0) {
    return 'Display name is required';
  }
  if (trimmed.length > DISPLAY_NAME_MAX_LENGTH) {
    return `Display name must be ${DISPLAY_NAME_MAX_LENGTH} characters or fewer`;
  }
  return null;
}
