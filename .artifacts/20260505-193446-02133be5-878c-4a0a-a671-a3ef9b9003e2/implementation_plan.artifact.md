# Implementation Plan - UI Fixes (Dark Mode & Back Button)

Fix the dark mode dialog background issue and standardize the back button design in the Book Appointment screen.

## Proposed Changes

### UI & Theming

#### [bg_dialog_rounded.xml](file:///C:/Users/Administrator/AndroidStudioProjects/CallAMechanic_Android_Application/app/src/main/res/drawable/bg_dialog_rounded.xml)

- Change `<solid android:color="@android:color/white" />` to `<solid android:color="@color/cam_white" />` so it respects dark mode colors.
- Ensure stroke uses `@color/cam_border`.

#### [activity_book_appointment.xml](file:///C:/Users/Administrator/AndroidStudioProjects/CallAMechanic_Android_Application/app/src/main/res/layout/activity_book_appointment.xml)

- Replace the `Button` (id: `btnCancel`) with a `LinearLayout` styled back button to match `ManageVehiclesActivity` and `RegisterActivity`. This will ensure the "Back to Dashboard" text is visible and styled correctly.

### Logic Refinement

#### [BookAppointmentActivity.kt](file:///C:/Users/Administrator/AndroidStudioProjects/CallAMechanic_Android_Application/app/src/main/java/com/jhonlauro/callamechanic/ui/client/BookAppointmentActivity.kt)

- No logic changes needed, as the ViewBinding will still pick up the ID if maintained (or I will update the ID to match the new container).

## Verification Plan

### Automated Tests
- I will run `./gradlew app:assembleDebug` to ensure the changes compile correctly.

### Manual Verification
- I will verify the "Back to Dashboard" text is visible on the new `LinearLayout` button.
- I will verify that `bg_dialog_rounded` now uses `@color/cam_white`, which will automatically switch to a dark background in dark mode.
