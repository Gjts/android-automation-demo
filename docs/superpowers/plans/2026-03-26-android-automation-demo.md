# Android Automation Demo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the agreed minimal demo where `targetapp` exposes a three-step UI flow and `automationapp` launches it and completes the fixed success path through an `AccessibilityService`.

**Architecture:** Keep both apps minimal and file-focused. `targetapp` stays as a single Compose activity that switches between Login / Decision / PIN screens via local state. `automationapp` keeps a simple launcher `MainActivity`, adds a small persisted armed flag, and delegates all cross-app interaction to a dedicated `AccessibilityService` plus one XML service config.

**Tech Stack:** Kotlin, Android SDK, Jetpack Compose, AccessibilityService, Android manifest/XML resources, Gradle

---

## File map

### Existing files to modify
- `src/mobile/targetapp/app/src/main/java/com/example/targetapp/MainActivity.kt`
  - Replace template greeting UI with the three-screen Compose flow.
- `src/mobile/automationapp/app/src/main/java/com/example/automationapp/MainActivity.kt`
  - Replace template greeting UI with a simple instruction screen and Start Demo button.
- `src/mobile/automationapp/app/src/main/AndroidManifest.xml`
  - Register the accessibility service and keep the launcher activity.

### New files to create
- `src/mobile/automationapp/app/src/main/java/com/example/automationapp/AutomationPreferences.kt`
  - Small helper for storing/clearing the armed flag.
- `src/mobile/automationapp/app/src/main/java/com/example/automationapp/DemoAccessibilityService.kt`
  - AccessibilityService implementation for launch-time automation.
- `src/mobile/automationapp/app/src/main/res/xml/accessibility_service_config.xml`
  - Accessibility service capabilities and scope.

### Verification commands
- `cd "C:/Users/13013/Desktop/test/android-automation-demo/src/mobile/targetapp" && ./gradlew :app:assembleDebug`
- `cd "C:/Users/13013/Desktop/test/android-automation-demo/src/mobile/automationapp" && ./gradlew :app:assembleDebug`

---

### Task 1: Implement the Target App three-screen flow

**Files:**
- Modify: `src/mobile/targetapp/app/src/main/java/com/example/targetapp/MainActivity.kt`

- [ ] **Step 1: Replace the template greeting with a local screen-state model**

Add a small screen enum or sealed class in `MainActivity.kt` with states for Login, Decision, and Pin.

- [ ] **Step 2: Add Compose state for password, error message, PIN value, and current screen**

Keep all state local to `MainActivity.kt`; do not add extra architecture layers.

- [ ] **Step 3: Build the Login screen UI**

Render exact visible text required by the spec:
- `Login Page`
- `Password`
- `Login`
- `Wrong password` (only after invalid submit)

Use a password-style text field.

- [ ] **Step 4: Implement Login button behavior**

On click:
- if password is `Test@2026`, clear any error and move to the Decision screen
- otherwise show `Wrong password`

- [ ] **Step 5: Build the Decision screen UI**

Render exact visible text:
- `Decision Page`
- `test text 1`
- `Test text`
- `Not test text`

- [ ] **Step 6: Implement Decision button behavior**

On click:
- `Test text` moves to the PIN screen
- `Not test text` finishes the activity

- [ ] **Step 7: Build the PIN screen UI**

Render exact visible text:
- `PIN Page`
- `PIN`
- numeric input field

Do not add any confirm / submit button on this screen.

- [ ] **Step 8: Implement PIN completion logic**

When the input equals `8526`, render the exact completion text `Automation complete`. Completion must happen immediately from the input value alone, with no extra tap required.

- [ ] **Step 9: Keep the file minimal**

Do not add extra files, navigation libraries, or abstractions. Keep everything in `MainActivity.kt` unless the file becomes unreasonably hard to read.

- [ ] **Step 10: Run the target app build**

Run:
```bash
cd "C:/Users/13013/Desktop/test/android-automation-demo/src/mobile/targetapp" && ./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

---

### Task 2: Add the Automation App launch UI and armed flag

**Files:**
- Modify: `src/mobile/automationapp/app/src/main/java/com/example/automationapp/MainActivity.kt`
- Create: `src/mobile/automationapp/app/src/main/java/com/example/automationapp/AutomationPreferences.kt`

- [ ] **Step 1: Create the armed-flag helper**

Add `AutomationPreferences.kt` with minimal functions to:
- set armed = true
- read armed
- clear armed

Keep it tiny; SharedPreferences is enough.

- [ ] **Step 2: Define repeat-run reset behavior**

Make `Start Demo` the beginning of a fresh run:
- when the user presses `Start Demo`, reset any persisted/held service progress back to the initial state before launching the target app
- the accessibility service must treat each new armed run as Login -> Decision -> PIN again

- [ ] **Step 3: Replace the template greeting in Automation MainActivity**

Render a simple Compose screen with:
- short instructions telling the user to enable accessibility service first
- one visible button such as `Start Demo`

- [ ] **Step 4: Implement Start Demo click behavior**

On click:
- reset service progress for a fresh run
- set armed = true
- fetch the launch intent for `com.example.targetapp`
- launch the target app if available

- [ ] **Step 5: Keep MainActivity non-magical**

Do not put accessibility traversal logic in the activity. It only resets the run, arms the run, and launches the target package.

- [ ] **Step 6: Run the automation app build after these changes**

Run:
```bash
cd "C:/Users/13013/Desktop/test/android-automation-demo/src/mobile/automationapp" && ./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

---

### Task 3: Implement the accessibility service and XML config

**Files:**
- Create: `src/mobile/automationapp/app/src/main/java/com/example/automationapp/DemoAccessibilityService.kt`
- Create: `src/mobile/automationapp/app/src/main/res/xml/accessibility_service_config.xml`

- [ ] **Step 1: Create the service class skeleton**

Create `DemoAccessibilityService.kt` extending `AccessibilityService` with:
- `onServiceConnected()` if useful for setup
- `onAccessibilityEvent()`
- `onInterrupt()`

- [ ] **Step 2: Add a tiny step-state model inside the service**

Track only what is needed:
- login action done
- decision action done
- pin action done

A simple enum is enough.

- [ ] **Step 3: Add minimal debounce handling**

Ignore rapid duplicate content-change handling based on a short timestamp window. Keep this local and simple.

- [ ] **Step 4: Gate automation by armed flag and package name**

In `onAccessibilityEvent()` return early unless:
- the armed flag is true
- the event package name is `com.example.targetapp`

- [ ] **Step 5: Implement helper search functions**

Add tiny helpers to find nodes by visible text and to locate editable fields/buttons from the active window root.

- [ ] **Step 6: Add deterministic accessibility fallback hooks in Target App UI**

If visible-text-only matching is not reliable in Compose, make field discovery deterministic inside `src/mobile/targetapp/app/src/main/java/com/example/targetapp/MainActivity.kt` by adding explicit semantics/content descriptions with these exact values:
- password input: `password_input`
- pin input: `pin_input`

Acceptance criteria:
- the Login screen still visibly shows `Password`
- the PIN screen still visibly shows `PIN`
- the service can deterministically distinguish the password field from the PIN field using either visible text or the exact fallback semantics/content descriptions above
- buttons remain recognisable by the exact visible texts `Login`, `Test text`, and `Not test text`
- do not add broad test-only plumbing or extra hidden controls

- [ ] **Step 7: Implement Login page automation**

When the root contains the exact texts needed for the Login screen:
- locate the editable password field
- set text to `Test@2026`
- click `Login`
- mark login step done

- [ ] **Step 8: Implement Decision page automation**

When the root shows `Decision Page` and `test text 1`:
- click `Test text`
- mark decision step done

- [ ] **Step 9: Implement PIN page automation**

When the root shows `PIN Page`:
- locate the editable PIN field
- set text to `8526`
- mark pin step done

- [ ] **Step 10: Implement completion handling**

When the root shows `Automation complete`:
- clear the armed flag
- reset in-memory step state so a future `Start Demo` begins from the Login step again
- stop taking further actions for this run

- [ ] **Step 11: Write the accessibility service XML config**

Create `accessibility_service_config.xml` with the spec-required settings:
- `typeWindowStateChanged|typeWindowContentChanged`
- `feedbackGeneric`
- `canRetrieveWindowContent=true`
- small `notificationTimeout`
- `packageNames="com.example.targetapp"`

- [ ] **Step 12: Run the automation app build**

Run:
```bash
cd "C:/Users/13013/Desktop/test/android-automation-demo/src/mobile/automationapp" && ./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

---

### Task 4: Register the service in the manifest

**Files:**
- Modify: `src/mobile/automationapp/app/src/main/AndroidManifest.xml`
- Reference: `src/mobile/automationapp/app/src/main/res/xml/accessibility_service_config.xml`

- [ ] **Step 1: Add the accessibility service declaration**

Register the service class under `<application>`.

- [ ] **Step 2: Add the required service attributes**

Set:
- `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`
- `android:exported="false"`

- [ ] **Step 3: Add the required intent-filter and metadata**

Inside the service declaration add:
- `<action android:name="android.accessibilityservice.AccessibilityService" />`
- `<meta-data android:name="android.accessibilityservice" android:resource="@xml/accessibility_service_config" />`

- [ ] **Step 4: Rebuild the automation app**

Run:
```bash
cd "C:/Users/13013/Desktop/test/android-automation-demo/src/mobile/automationapp" && ./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`

---

### Task 5: Verify the complete demo path

**Files:**
- Verify: `src/mobile/targetapp/app/src/main/java/com/example/targetapp/MainActivity.kt`
- Verify: `src/mobile/automationapp/app/src/main/java/com/example/automationapp/MainActivity.kt`
- Verify: `src/mobile/automationapp/app/src/main/java/com/example/automationapp/DemoAccessibilityService.kt`
- Verify: `src/mobile/automationapp/app/src/main/AndroidManifest.xml`
- Verify: `src/mobile/automationapp/app/src/main/res/xml/accessibility_service_config.xml`

- [ ] **Step 1: Run both assembleDebug builds again**

Run both commands:
```bash
cd "C:/Users/13013/Desktop/test/android-automation-demo/src/mobile/targetapp" && ./gradlew :app:assembleDebug
cd "C:/Users/13013/Desktop/test/android-automation-demo/src/mobile/automationapp" && ./gradlew :app:assembleDebug
```
Expected: both succeed.

- [ ] **Step 2: Install both apps on a device/emulator**

Use Android Studio or adb.

- [ ] **Step 3: Enable the accessibility service manually**

In system settings, enable the Automation App accessibility service.

- [ ] **Step 4: Run the manual demo flow**

Open Automation App, press `Start Demo`, and confirm the following visible transitions happen in order:
- `Login Page`
- password set to `Test@2026`
- `Login` clicked
- `Decision Page`
- `Test text` clicked
- `PIN Page`
- PIN set to `8526`
- `Automation complete`

- [ ] **Step 5: Confirm automation stops after completion**

Verify the service clears armed state and does not continue clicking or typing after the completion text appears.

---

### Task 6: Minimal README alignment

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update the documented project structure**

Replace the outdated top-level `target-app/` / `automation-app/` structure with the actual `src/mobile/targetapp` and `src/mobile/automationapp` paths.

- [ ] **Step 2: Keep README edits minimal**

Only fix repo-structure and implementation-alignment issues needed so the README no longer contradicts the built demo.

- [ ] **Step 3: Re-read README for consistency**

Make sure the README still describes the same success-path demo that the code implements.

---

## Notes for the implementing agent

- Keep the solution intentionally simple.
- Do not add navigation libraries, ViewModels, dependency injection, or a broad refactor.
- Prefer visible-text matching over ViewId contracts for this demo, because the spec explicitly defines visible labels.
- If Compose accessibility matching is flaky, add only the minimum semantics/content descriptions needed to make the fixed path reliable.
- Do not expand scope into random branches, retry frameworks, or test harnesses.
