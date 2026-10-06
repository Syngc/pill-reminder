# Mobile (Android)

Kotlin + Jetpack Compose. Min SDK 26, target SDK 35. Fully bilingual: Spanish and English.

## Build and run

Requires JDK 17+ (Android Studio's bundled JBR works) and the Android SDK.

```bash
./gradlew installDebug        # build and install on a running emulator/device
./gradlew testDebugUnitTest   # unit tests
```

Settings go in `mobile/local.properties` (git-ignored, never committed):

```properties
backend.url=http://10.0.2.2:8000         # debug builds; the emulator's alias for your machine
backend.releaseUrl=https://…run.app      # release builds; must be https
backend.apiKey=…                         # must match the backend's APP_API_KEY
release.storeFile=/Users/…/.android/pill-reminder-release.jks
release.storePassword=…
release.keyAlias=pill-reminder
release.keyPassword=…
```

### Release APK

```bash
./gradlew assembleRelease    # → app/build/outputs/apk/release/MedRing-<version>-release.apk
```

The release build refuses to run without an `https://` backend URL and the signing key. **Back up the keystore file and its password together** (for example, in a password manager). Android installs an update only if it is signed with the same key, so losing the key means everyone has to uninstall and reinstall, losing their saved medicines. Bump `versionCode` in `app/build.gradle.kts` for every APK you hand out.

On a physical phone, use your machine's LAN IP or a deployed HTTPS URL. Cleartext HTTP is only allowed to `10.0.2.2` and `localhost`; see `res/xml/network_security_config.xml`.

## How it works

| Piece | File |
| --- | --- |
| Home: today's doses and status, medicines, test alarm | `ui/home/` |
| Add prescription: camera/gallery → backend → review/edit → confirm | `ui/add/` |
| Full-screen alarm over the lock screen with one big button | `ui/AlarmActivity.kt` |
| One exact alarm per time of day (meds due together ring together) | `alarm/AlarmScheduler.kt` |
| Ringing: alarm tone alternating with Spanish speech, vibration | `alarm/AlarmService.kt`, `alarm/Speaker.kt` |
| What is spoken | `alarm/DoseMessage.kt` |
| Re-arming alarms after reboot, time or timezone change | `alarm/BootReceiver.kt` |
| Storage (medicines, dose log) | `data/` (Room) |

Alarm behavior: rings for up to 3 minutes. If nobody confirms, it rings again after 10 minutes, up to 3 attempts, and then the dose is logged as *No confirmada*.

## Design

The app is called **MedRing** in both languages (`app_name`, not translated). Its launcher icon is the website's pill: a white outlined capsule on the accent blue (`res/drawable/ic_launcher_foreground.xml`), with a monochrome layer for Android 13+ themed icons; the notification icon uses the same shape.


The UI follows the "MedRing Redesign" canvas from Claude Design: a blue palette with a matching dark mode, text no smaller than 18sp, one main action per screen, and every status shown as icon + word + color ("Taken", "Now", "Later", "Not confirmed"). Colors and type live in `ui/theme/Theme.kt`, shared pieces (buttons, badges, chips, banners, icons) in `ui/components/`.

The typeface is **Atkinson Hyperlegible Next**, designed for readers with low vision, bundled as `res/font/atkinson_hyperlegible_next.ttf` under the SIL Open Font License (`licenses/AtkinsonHyperlegibleNext-OFL.txt`).

Home shows the dose due **now** (a ringing dose, or one whose time passed less than an hour ago) with **I took it** and **Read it out loud**; confirming there also stops a ringing alarm and cancels its retries. The alarm screen has **Say it again**, which cuts the tone short and repeats the spoken message.

## Languages

The person picks Español or English with the language button on the home screen. The choice is saved in the app's own settings, separate from the phone's language, so the alarm service knows which language to use even when it starts with no screen open.

| What | Where |
| --- | --- |
| Screen text | `res/values/strings.xml` (Spanish, the default) and `res/values-en/strings.xml`. Lint fails the build if a key is missing from either. |
| Spoken alarm script and the big button label | `alarm/DoseMessage.kt` (singular/plural per language, unit-tested) |
| Voice | `alarm/Speaker.kt` prefers es-MX/es-US/es-419/es-ES or en-US/en-GB |
| AI output and server errors | `Accept-Language` header sent by `network/ExtractionApi.kt` |
| Applying the choice to screens | `ui/LocalizedActivity.kt`, `data/AppLanguage.kt` |

Medicines saved earlier keep their instructions in the language they were saved in; switching languages does not re-translate them. Language splitting is turned off for App Bundles (`bundle.language.enableSplit = false`) so Google Play installs both languages on every phone. The launcher icon's label follows the phone's system language, because Android doesn't let the app control it.

To add a language: add `values-xx/strings.xml`, an `AppLanguage` entry, its branches in `DoseMessage` and `Speaker.candidateLocales`, and its messages in `backend/app/i18n.py`.

## Permissions

- `USE_EXACT_ALARM` makes alarms fire at the exact minute. Google Play limits this permission to alarm and calendar apps. A medication alarm fits, but the Play listing will need a declaration.
- `USE_FULL_SCREEN_INTENT` shows the alarm over the lock screen. On Android 14+ the user may need to allow it; the home screen shows a banner when it's missing.
- `POST_NOTIFICATIONS` is requested on first launch (Android 13+).
- `FOREGROUND_SERVICE_SYSTEM_EXEMPTED` covers the ringing service, which is allowed for apps that hold the exact-alarm permission.

## Verified on the Pixel 8 emulator (API 35)

- An exact alarm fired at the scheduled minute while the phone was locked. It woke the screen, showed full screen, and spoke through the `es-US` voice.
- Switching languages updates every screen at once. With the phone in English and the app set to Spanish, an alarm started from a cold process was Spanish throughout (screen, notification, voice), and the reverse was English throughout (`en-US` voice).
- The big button logged the dose as *Tomada*, stopped the ringing, and the alarm re-armed for the next day.

Not yet tested: photo → backend → review against a live backend, a physical device, and OEM battery savers (some brands, like Xiaomi and Samsung, may delay alarms unless the app is exempted).
