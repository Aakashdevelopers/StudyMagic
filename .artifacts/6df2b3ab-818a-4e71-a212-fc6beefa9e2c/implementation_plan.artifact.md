# Google Play Console Production Readiness Plan - Study Magic

Perform a complete production-readiness audit and fix identified issues for "Study Magic".

## User Review Required

> [!IMPORTANT]
> **Hardcoded AI API Key**: The `OPENROUTER_KEY` is currently hardcoded in the client. For production, this should be moved to a backend service to prevent exposure and potential misuse (and unexpected costs). I will remove it from the code and add a fallback, but the developer **must** implement a backend proxy for this.

> [!IMPORTANT]
> **Supabase RLS**: While the Supabase key found is a publishable key (anon), the developer **must** verify that Row Level Security (RLS) is enabled on all tables (especially `notes` if it has an `insert` method) to prevent unauthorized data modification.

## Proposed Changes

### Build Configuration & Manifest

#### [MODIFY] [build.gradle.kts](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/build.gradle.kts)
- Downgrade `compileSdk` and `targetSdk` to 35 (Android 15) for stability and compliance.
- Enable `isMinifyEnabled` and `isShrinkResources` in the release build.
- Update `versionCode` and `versionName` if necessary (placeholder 1 and "1.0").

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/AndroidManifest.xml)
- Remove `READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE` permissions as they are not used.
- Add `android:label` to activities if missing (already present for app).
- Ensure `android:exported` is only used where necessary.

---

### Security & Privacy

#### [MODIFY] [Constants.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/utils/Constants.java)
- Remove the hardcoded `OPENROUTER_KEY`.
- Provide instructions for backend integration.

#### [MODIFY] [ApiClient.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/api/ApiClient.java)
- Change `HttpLoggingInterceptor` level to `Level.NONE` or `Level.BASIC` for production.
- Use `BuildConfig.DEBUG` to enable body logging only during development.

#### [MODIFY] [ResultFragment.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/fragments/ResultFragment.java)
- Add a check for the AI API key and show a user-friendly message if it's missing or if the service is unavailable.

---

### Legal & Compliance Artifacts

#### [NEW] [privacy_policy.artifact.md](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/.artifacts/6df2b3ab-818a-4e71-a212-fc6beefa9e2c/privacy_policy.artifact.md) [NEW]
- Create a production-ready privacy policy tailored to the app's actual data usage (Supabase, OpenRouter).

#### [NEW] [terms_and_conditions.artifact.md](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/.artifacts/6df2b3ab-818a-4e71-a212-fc6beefa9e2c/terms_and_conditions.artifact.md) [NEW]
- Create professional terms and conditions.

#### [NEW] [data_safety.artifact.md](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/.artifacts/6df2b3ab-818a-4e71-a212-fc6beefa9e2c/data_safety.artifact.md) [NEW]
- Prepare the Play Console Data Safety questionnaire answers.

## Verification Plan

### Automated Tests
- `gradlew assembleRelease` to verify minification and build stability.
- `gradlew lint` to check for any new issues.

### Manual Verification
- Verify that the app still loads categories and tests from Supabase.
- Verify that the PDF viewer still works without storage permissions.
- Verify that the AI analysis fails gracefully if the key is removed.
