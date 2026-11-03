# Ascend

Native Android fitness and nutrition planner for university students, built with Kotlin and Jetpack Compose.

## Run in Android Studio
1. Clone this repository and open its root folder.
2. Use JDK 17 and allow Gradle sync.
3. Run the app configuration on Android 8.0 (API 26) or newer.

## Prototype screens
- Today: daily calorie summary, persistent meal completion, and user-created meals
- Plan: selectable date preview and meal list
- Recipes: Budget / Standard / Premium filtering with sample ingredient costs
- Profile: dietary preferences, persistent hostel mode, and Ascend Plus preview

## Local persistence
- Room (SQLite) stores meal completion by date and meal ID.
- DataStore stores hostel mode and selected recipe budget tier.
- Data remains on the device and is removed if app data is cleared or the app is uninstalled.
- No account, cloud backup, or cross-device synchronization is implemented.

## Prototype limitations
Meals, macros, and rupee costs are sample data, not verified nutrition analysis or live market pricing. Authentication, backend sync, calendar notifications, expert marketplace, moderation, and payment processing are not implemented. Nutrition targets are not medical advice.

## Next architecture steps
Add editable meal plans and recipe persistence, then backend API/authentication, verified nutrition data, region-specific price sourcing, expert credential review, and compliant subscription/payment integrations.
