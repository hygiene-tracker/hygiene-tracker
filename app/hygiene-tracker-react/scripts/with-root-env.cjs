const fs = require('node:fs');
const path = require('node:path');
const { spawn } = require('node:child_process');

const rootEnvPath = path.resolve(__dirname, '../../../.env');
const appEnvKeys = [
  'EXPO_PUBLIC_API_BASE_URL',
  'EXPO_PUBLIC_DEBUG_API',
  'EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID',
  'EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID',
  'EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID',
  'EXPO_PUBLIC_GITHUB_CLIENT_ID',
  'EXPO_PUBLIC_GITHUB_REDIRECT_URI',
];

function loadRootEnv(filePath) {
  if (!fs.existsSync(filePath)) {
    return;
  }

  for (const line of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const match = line.match(/^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$/);
    if (!match) {
      continue;
    }

    let value = match[2];
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    } else {
      value = value.replace(/\s+#.*$/, '').trim();
    }

    // Set these before Expo starts so the root .env takes precedence over the
    // app directory's automatically loaded .env file.
    process.env[match[1]] = value;
  }
}

loadRootEnv(rootEnvPath);

// Prevent Expo's automatic .env loading from filling missing app keys from
// app/hygiene-tracker-react/.env. An unset root value intentionally stays empty.
for (const key of appEnvKeys) {
  process.env[key] ??= '';
}

const expoCommand = process.platform === 'win32' ? 'expo.cmd' : 'expo';
const child = spawn(expoCommand, process.argv.slice(2), {
  env: process.env,
  stdio: 'inherit',
});

child.on('exit', (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal);
  } else {
    process.exit(code ?? 1);
  }
});

child.on('error', (error) => {
  console.error(`Could not start Expo: ${error.message}`);
  process.exit(1);
});
