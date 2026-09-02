# Implementation Plan - Uniform Status Bar Style

The goal is to make the status bar consistent across all fragments and activities, following the style of `HomeFragment` (transparent status bar with light icons and a dark primary-colored header).

## User Review Required

> [!IMPORTANT]
> Changing the status bar icons to light (white) requires the top background of every screen to be dark (using `@color/colorPrimary`). I will update `NotesFragment` and the Quiz activities to have a dark header to match this style.

## Proposed Changes

### [Fragment Layouts]

#### [MODIFY] [fragment_notes.xml](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/res/layout/fragment_notes.xml)
- Wrap the top "Study Notes" title in a dark header `LinearLayout` using `@drawable/bg_home_header`.
- Adjust padding to accommodate the status bar.

### [Fragment Code]

#### [MODIFY] [NotesFragment.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/fragments/NotesFragment.java)
- Change `setLightStatusBar(getActivity(), true)` to `false`.
- Ensure `applyTopInset` is called on the new header view.

### [Activities]

#### [MODIFY] [MainActivity.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/MainActivity.java)
- Set default status bar icons to light (`false`) in `onCreate`.

#### [MODIFY] [QuizActivity.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/QuizActivity.java)
- Change status bar icons to light (`false`).
- Update the top bar layout in XML to use a dark background (optional but recommended for visibility).

#### [MODIFY] [MockupQuizActivity.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/MockupQuizActivity.java)
- Change status bar icons to light (`false`).
- Update the top bar layout in XML to use a dark background.

#### [MODIFY] [activity_quiz.xml](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/res/layout/activity_quiz.xml) and [activity_mockup_quiz.xml](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/res/layout/activity_mockup_quiz.xml)
- Change `llTopBar` background to `@color/colorPrimary`.
- Update text and icon colors in the top bar to white for contrast.

## Verification Plan

### Manual Verification
- Deploy the app and navigate through all bottom navigation tabs: Home, Exams, Notes, and About.
- Verify that the status bar remains transparent with light icons and a dark header on all screens.
- Open a quiz and verify the status bar style matches.
- Open a PDF and verify the status bar style.
