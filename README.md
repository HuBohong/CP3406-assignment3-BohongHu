# CodeQuest

CodeQuest is an Android app that teaches Kotlin fundamentals to beginner programmers
(university students starting their first programming subject). Learners work along a path
of short lessons, in the style of Duolingo, by reading code, predicting its output and
rebuilding scrambled programs.

Built for **CP3406 Assessment 3: Education App** (James Cook University) by Bohong Hu.

## Core features

| Feature | What it does |
|---|---|
| Learning path | 4 units, 8 lessons and 40 questions on variables, types, conditionals, loops, functions, lambdas, collections and null safety. A lesson opens when the one before it is finished. |
| Two question types | **Choice** questions (concepts, "what does this print?", fill the blank) and **ordering** questions, where the learner taps scrambled lines of code into a working order. |
| Mastery, not lives | A wrong answer is explained and the question returns at the end of the lesson. There are no hearts, timers or penalties; mistakes only affect the 1 to 3 star rating. |
| Bonus round | Downloads fresh computing questions from the Open Trivia DB API. The last downloaded round is cached, so it can be replayed offline. |
| Statistics | Lessons completed, XP, first-try accuracy, days practised, accuracy by topic and recent sessions. |
| Settings | Sound, bonus difficulty, theme, unlock-all-lessons, answer-history consent and "delete my progress". |

## Screens

| Screen | Purpose | Code |
|---|---|---|
| Onboarding | First launch only. Explains what is stored and asks one optional consent question. | `ui/onboarding` |
| Landing page (Learn) | Progress summary, "Continue" shortcut, bonus round and the lesson path. | `ui/home` |
| Activity (Lesson) | The quiz itself, with feedback after every answer. Also used for the bonus round. | `ui/lesson` |
| Result | Stars, XP and first-try score for the finished lesson. | `ui/result` |
| User statistics (Progress) | Headline numbers, accuracy by topic, recent sessions. | `ui/stats` |
| Settings | Preferences and privacy controls. | `ui/settings` |

## How it is built

### Architecture

The app follows the layered architecture recommended in the Android guide. Each layer only
knows about the one below it.

```
ui (Compose screens + ViewModels, one package per screen)
        |  StateFlow<UiState> up, events down
data/repository (interfaces + implementations: the single source of truth)
        |
data/local (Room)   data/remote (Retrofit)   DataStore
        
domain (pure Kotlin: models, QuizSession, Scoring, Progression) - used by every layer
```

- **Domain layer** (`domain/`) has no Android imports. `QuizSession` is an immutable state
  machine for a lesson, `Scoring` turns a result into stars and XP, and `Progression` decides
  which lessons are open. This is where most unit tests are aimed.
- **Repositories** (`data/repository/`) are interfaces. ViewModels depend on the interface,
  and Hilt binds the real implementation (`di/RepositoryModule.kt`). Tests bind fakes.
- **ViewModels** expose one immutable `UiState` as a `StateFlow`, built with `combine` and
  `stateIn(WhileSubscribed)` so database work stops when the screen is not visible.
- **Screens** are split into a `Route` composable (gets the ViewModel) and a stateless
  `Screen` composable (takes state and lambdas), which is what the UI tests render.
- **Dependency injection** uses Hilt: `@HiltAndroidApp`, `@AndroidEntryPoint`,
  `@HiltViewModel`, a `@Provides` module for Room, Retrofit and DataStore, and a `@Binds`
  module for repositories.

### Navigation

`ui/navigation/CodeQuestNavigation.kt` uses Navigation Compose with a single `NavHost`.

- Three top-level tabs (Learn, Progress, Settings) with saved and restored tab state.
- **Adaptive**: a bottom navigation bar on phones and a navigation rail from 600 dp width.
- `lesson/{lessonId}` takes a string argument that `LessonViewModel` reads from
  `SavedStateHandle`. `result/...` takes typed integer arguments.
- Back stack control: onboarding is removed once finished, and a finished lesson is replaced
  by its result so Back never reopens a completed quiz.
- A deep link, `codequest://lesson/{lessonId}`, opens a lesson directly. Try it with
  `adb shell am start -a android.intent.action.VIEW -d "codequest://lesson/basics-1"`.
- Leaving a lesson half-way asks for confirmation through `BackHandler`.

### Persistence (Room and DataStore)

`data/local/` defines six Room tables:

| Table | Content |
|---|---|
| `lessons`, `questions` | The course, copied from `SeedData` on first launch. Questions reference lessons with a cascading foreign key. Option lists are stored through a `TypeConverter`. |
| `lesson_progress` | Best stars and completion count per lesson (`@Upsert`). |
| `sessions` | One row per finished lesson or bonus round. |
| `attempts` | One row per answer. Written only if the learner opted in. |
| `trivia_cache` | The last downloaded bonus round for each difficulty. |

Queries worth a look: a `LEFT JOIN` that returns lessons with progress as a `Flow`,
`GROUP BY` topic accuracy, a distinct-calendar-day count with `strftime`, and `@Transaction`
methods that save a session atomically and keep the best star rating.

Preferences (theme, sound, difficulty, consent) are stored with Preferences DataStore.

### Networking

`data/remote/TriviaApi.kt` is a Retrofit interface for
[Open Trivia DB](https://opentdb.com) (category 18, "Science: Computers"), parsed with
kotlinx.serialization. `NetworkTriviaRepository` is network-first with a Room fallback: a
successful download replaces the cache, and any failure (offline, timeout, rate limit) falls
back to the cached round. The API needs no key and the request contains no user data.

### Libraries

Kotlin 2.0, Jetpack Compose with Material 3, Navigation Compose, Hilt, Room, DataStore,
Retrofit with OkHttp, kotlinx.serialization and coroutines/Flow. Versions are in
`gradle/libs.versions.toml`.

## Ethical and professional design

The app applies the issues researched in Assessment 2 (data privacy, dark patterns and the
ACS Codes).

| Decision | Where to see it |
|---|---|
| **Privacy by default.** No answer history is kept until the learner opts in. | `UserSettings` defaults, `LessonViewModel.finish` |
| **Equal-weight consent.** Accept and Decline use the same button, size and weight, and nothing is preselected. A UI test asserts the two buttons are the same size. | `OnboardingScreen`, `OnboardingScreenTest` |
| **Declining never blocks learning.** Every lesson works without the optional history. | `StatsScreen` (explains what is unavailable and why) |
| **Withdrawing consent deletes the data** collected under it. | `SettingsViewModel.setDetailedHistory` |
| **Data minimisation.** No account, no ads, no analytics SDK. The only permission is `INTERNET`. | `AndroidManifest.xml` |
| **Data stays on the device.** Auto Backup and device transfer are switched off. | `data_extraction_rules.xml`, `backup_rules.xml` |
| **No addictive design.** No streaks to lose, lives, countdowns or reminder notifications. "Days practised" only ever goes up. | `ResultScreen`, `ProgressDao.observeDaysPractised` |
| **User autonomy.** The learner can unlock all lessons, change consent and delete all progress at any time. | `SettingsScreen` |
| **Accessibility.** TalkBack labels and headings, feedback announced as a live region, results shown with icons as well as colour, 48 dp touch targets, light and dark themes. | `ui/` |
| **Transparency.** The privacy summary and the data source and licence are shown in Settings. | `SettingsScreen` |

### Not implemented

Two ideas from the Assessment 2 presentation were left out of this version:

- **Keystore encryption of progress.** The app stores no personal identifiers, the database
  is in app-private storage and backup is disabled, so encryption was judged a low priority.
- **Attaching an image with the photo picker.** The final feature set has no need for
  learner images, so the most private option was to request no media access at all.

## Testing

Local JVM tests (`app/src/test`), run with `./gradlew test`:

| Test class | Covers |
|---|---|
| `QuizSessionTest`, `ScoringTest`, `ProgressionTest` | Quiz rules, stars and XP, lesson unlocking |
| `TriviaQuestionsTest`, `SeedDataTest` | API text decoding and the integrity of all 40 built-in questions |
| `TriviaRepositoryTest` | Network-first loading, cache fallback, JSON parsing |
| `LessonViewModelTest`, `HomeViewModelTest`, `SettingsViewModelTest` | ViewModel state and the consent logic, using fake repositories |

Instrumented tests (`app/src/androidTest`), run with `./gradlew connectedAndroidTest` on an
emulator or device:

| Test class | Covers |
|---|---|
| `ProgressDaoTest` | Real Room queries against an in-memory database |
| `OnboardingScreenTest`, `LessonScreenTest` | Compose GUI tests of the consent and activity screens |

## Running the app

1. Open the project folder in Android Studio and let Gradle sync.
2. Run the `app` configuration on an emulator or device with Android 8.0 (API 26) or newer.
3. The bonus round needs an internet connection. Everything else works offline.

## Credits

Bonus-round questions come from [Open Trivia DB](https://opentdb.com), licensed
CC BY-SA 4.0. The Kotlin course content was written for this project.
