import { describe, expect, it } from '@jest/globals';

import { DISPLAY_NAME_MAX_LENGTH, validateDisplayName } from './display-name';

describe('validateDisplayName', () => {
  it('requires a display name when it is empty', () => {
    expect(validateDisplayName('')).toBe('Display name is required');
  });

  it('requires a display name when it is only spaces', () => {
    expect(validateDisplayName('   ')).toBe('Display name is required');
  });

  it('accepts a valid display name', () => {
    expect(validateDisplayName('Fresh Prince')).toBeNull();
  });

  it('accepts exactly the maximum length', () => {
    expect(validateDisplayName('a'.repeat(DISPLAY_NAME_MAX_LENGTH))).toBeNull();
  });

  it('rejects a display name over the maximum length', () => {
    expect(validateDisplayName('a'.repeat(DISPLAY_NAME_MAX_LENGTH + 1))).toBe(
      'Display name must be 32 characters or fewer'
    );
  });

  it('ignores spaces around the name when checking the length', () => {
    expect(validateDisplayName(`  ${'a'.repeat(DISPLAY_NAME_MAX_LENGTH)}  `)).toBeNull();
  });
});
