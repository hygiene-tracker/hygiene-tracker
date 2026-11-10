# Welcome to your Expo app 👋

This is an [Expo](https://expo.dev) project created with [`create-expo-app`](https://www.npmjs.com/package/create-expo-app).

## Get started

1. Install dependencies

   ```bash
   npm install
   ```

2. Start the app

   ```bash
   npx expo start
   ```

In the output, you'll find options to open the app in a

- [development build](https://docs.expo.dev/develop/development-builds/introduction/)
- [Android emulator](https://docs.expo.dev/workflow/android-studio-emulator/)
- [iOS simulator](https://docs.expo.dev/workflow/ios-simulator/)
- [Expo Go](https://expo.dev/go), a limited sandbox for trying out app development with Expo

You can start developing by editing the files inside the **src/app** directory. This project uses [file-based routing](https://docs.expo.dev/router/introduction).

## Check the Spring Boot API connection

Start the API in another terminal (`cd api && sh ./gradlew bootRun`), then run this from the React app directory:

```bash
npm run test:api
```

This calls the public `GET /api/health` endpoint and does not send an auth token. It defaults to `http://localhost:8080`. To use a different API address, set `API_BASE_URL`, for example:

```bash
API_BASE_URL=http://192.168.1.25:8080 npm run test:api
```

## Google ID token handoff

Copy `.env.example` to `.env` and set the Google Web, Android, and iOS OAuth client IDs for your app. The Explore tab has a **Continue with Google** test button: after Google returns an ID token, the app posts it to `POST /api/auth/google` and shows the identity returned by Spring Boot.

Configure Spring Boot to accept those same client IDs as token audiences, separated by commas:

```bash
cd api
GOOGLE_CLIENT_IDS="your-web-client-id, your-android-client-id, your-ios-client-id" sh ./gradlew bootRun
```

Restart Expo after changing `.env`. For an Android emulator, set `EXPO_PUBLIC_API_BASE_URL=http://10.0.2.2:8080`; for a physical phone, use the computer's LAN IP address. Google Android/iOS OAuth clients must also match the app's package/bundle identifiers and signing configuration.

## Get a fresh project

When you're ready, run:

```bash
npm run reset-project
```

This command will move the starter code to the **example** directory and create a blank **src/app** directory where you can start developing.

### Other setup steps

- To set up ESLint for linting, run `npx expo lint`, or follow our guide on ["Using ESLint and Prettier"](https://docs.expo.dev/guides/using-eslint/)
- If you'd like to set up unit testing, follow our guide on ["Unit Testing with Jest"](https://docs.expo.dev/develop/unit-testing/)
- Learn more about the TypeScript setup in this template in our guide on ["Using TypeScript"](https://docs.expo.dev/guides/typescript/)

## Learn more

To learn more about developing your project with Expo, look at the following resources:

- [Expo documentation](https://docs.expo.dev/): Learn fundamentals, or go into advanced topics with our [guides](https://docs.expo.dev/guides).
- [Learn Expo tutorial](https://docs.expo.dev/tutorial/introduction/): Follow a step-by-step tutorial where you'll create a project that runs on Android, iOS, and the web.

## Join the community

Join our community of developers creating universal apps.

- [Expo on GitHub](https://github.com/expo/expo): View our open source platform and contribute.
- [Discord community](https://chat.expo.dev): Chat with Expo users and ask questions.
