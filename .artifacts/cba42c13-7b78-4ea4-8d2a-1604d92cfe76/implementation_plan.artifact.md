# Implementation Plan - Fix Edge-to-Edge Overlap Issues

The app is experiencing overlap issues with the status bar and navigation bar on some devices (likely those running Android 15 where edge-to-edge is enabled by default). We need to explicitly enable edge-to-edge support and handle window insets to provide appropriate padding for the top and bottom UI elements.

## Proposed Changes

### [Component] Quiz Screen

#### [MODIFY] [QuizActivity.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/QuizActivity.java)
- Import `EdgeToEdge`, `ViewCompat`, and `WindowInsetsCompat`.
- Call `EdgeToEdge.enable(this)` in `onCreate`.
- Implement `setOnApplyWindowInsetsListener` for `llTopBar` to add top padding for the status bar.
- Implement `setOnApplyWindowInsetsListener` for `llBottomActions` to add bottom padding for the navigation bar.

#### [MODIFY] [activity_quiz.xml](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/res/layout/activity_quiz.xml)
- Change `llTopBar` height from `64dp` to `wrap_content` and add `android:minHeight="64dp"` to ensure it expands correctly when top padding is added.

### [Component] Main Screen

#### [MODIFY] [MainActivity.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/MainActivity.java)
- Call `EdgeToEdge.enable(this)` in `onCreate`.
- Handle insets for the root layout to ensure the bottom navigation bar doesn't overlap with the system navigation bar.

## Verification Plan

### Automated Tests
- Build the project to ensure no compilation errors with the new imports and API calls.

### Manual Verification
- Deploy to an Android 15 (API 35) emulator or device.
- Verify that the Top Bar in QuizActivity is below the status bar icons.
- Verify that the Bottom Actions in QuizActivity are above the system navigation bar.
- Verify similar behavior in MainActivity.
