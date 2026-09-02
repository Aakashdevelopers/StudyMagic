# Remove Ask AI Activity

This plan outlines the removal of the "Ask AI" feature from the Study Magic app, including the Activity, its layouts, and related unused adapters and models.

## Proposed Changes

### [app]

#### [DELETE] [AskAIActivity.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/AskAIActivity.java)
#### [DELETE] [activity_ask_ai.xml](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/res/layout/activity_ask_ai.xml)
#### [DELETE] [ChatAdapter.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/adapters/ChatAdapter.java)
#### [DELETE] [item_chat_user.xml](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/res/layout/item_chat_user.xml)
#### [DELETE] [item_chat_ai.xml](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/res/layout/item_chat_ai.xml)
#### [DELETE] [ChatMessage.java](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/java/com/amstudio/studymagic/models/ai/ChatMessage.java)

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Harish Computer/AndroidStudioProjects/StudyMagic/app/src/main/AndroidManifest.xml)
- Remove the `<activity android:name=".AskAIActivity" ... />` declaration.

## Verification Plan

### Automated Tests
- Run a build to ensure no compilation errors after deletion.
- Verify that no other components were accidentally broken.

### Manual Verification
- Deploy the app to a device/emulator and ensure it still runs correctly.
- Verify that any former entry points to "Ask AI" (if any) are either gone or don't cause crashes. (Note: No entry points were found in the current codebase).
