# Implementation Plan - Responsive UI and System Bar Insets Fix

This plan addresses the overlapping UI issues and lack of responsiveness across different Android devices by implementing modern `WindowInsets` handling and refactoring layouts to be more flexible.

## User Review Required

> [!IMPORTANT]
> The app will now use `EdgeToEdge` by default. This means the app content will draw behind the system bars (status bar and navigation bar). I will handle the insets manually to ensure no overlapping occurs.

## Proposed Changes

### [Component] Core Infrastructure

#### [NEW] [WindowInsetsUtil.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/utils/WindowInsetsUtil.java)
Create a utility class to handle `WindowInsets` consistently across the app. This will provide methods to:
- Apply top padding for the status bar.
- Apply bottom padding for the navigation bar.
- Handle IME (keyboard) insets.
- Avoid double-applying insets.

#### [MODIFY] [themes.xml](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/res/values/themes.xml)
- Update the base theme to support `EdgeToEdge` better.
- Set `android:windowLayoutInDisplayCutoutMode` to `shortEdges` for notch support.
- Ensure system bar icons have correct contrast (light/dark).

### [Component] Main Navigation

#### [MODIFY] [MainActivity.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/MainActivity.java)
- Ensure `EdgeToEdge.enable(this)` is called.
- Use `WindowInsetsUtil` to handle bottom navigation insets and ensure the fragment container also respects top insets if needed.

#### [MODIFY] [activity_main.xml](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/res/layout/activity_main.xml)
- Ensure the fragment container is constrained correctly.

### [Component] Exam/Quiz Experience

#### [MODIFY] [QuizActivity.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/QuizActivity.java)
- Use `WindowInsetsUtil` for `llTopBar` and `llBottomActions`.
- Ensure `EdgeToEdge` is handled properly without overwriting existing padding.

#### [MODIFY] [MockupQuizActivity.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/MockupQuizActivity.java)
- Enable `EdgeToEdge`.
- Implement `WindowInsets` handling for top and bottom bars.

#### [MODIFY] [activity_quiz.xml](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/res/layout/activity_quiz.xml)
- Refactor to use `ConstraintLayout` more effectively.
- Replace fixed heights with `wrap_content` or flexible constraints.
- Ensure the question content is in a `ScrollView` that spans the available space between the top bar and bottom actions.

#### [MODIFY] [activity_mockup_quiz.xml](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/res/layout/activity_mockup_quiz.xml)
- Similar refactoring as `activity_quiz.xml`.

### [Component] Other Screens

#### [MODIFY] [HomeFragment.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/fragments/HomeFragment.java)
- Handle top insets for the header area.

#### [MODIFY] [fragment_home.xml](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/res/layout/fragment_home.xml)
- Remove hardcoded `paddingTop` in the header.
- Ensure the `NestedScrollView` handles its content properly.

#### [MODIFY] [AskAIActivity.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/AskAIActivity.java)
- Enable `EdgeToEdge`.
- Handle insets for the chat input and top bar.
- Ensure the keyboard (IME) doesn't overlap the input field.

#### [MODIFY] [PdfViewerActivity.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/PdfViewerActivity.java)
- Enable `EdgeToEdge`.
- Handle insets for the toolbar.

## Verification Plan

### Automated Tests
- I will perform a build to ensure no syntax errors.

### Manual Verification
- Mentally simulate rendering on different screen sizes (360dp, 412dp).
- Verify `WindowInsets` application in the IDE's Layout Editor (using System UI preview).
- Check that `EdgeToEdge` logic covers both status bar (top) and navigation bar (bottom).
- Ensure typography scales correctly using `sp`.
- Verify scrollability in Quiz and AI screens.
