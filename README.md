# Ascend

Native Android fitness and nutrition planner for university students, built with Kotlin and Jetpack Compose.

## Run in Android Studio
1. Clone this repository and open its root folder.
2. Use JDK 17 and allow Gradle sync.
3. Run the app configuration on Android 8.0 (API 26) or newer.

## Prototype screens
- Today: daily calorie/macro summary and meal completion checklist
- Plan: selectable date preview and meal list
- Recipes: Budget / Standard / Premium filtering with sample ingredient costs
- Profile: dietary preferences, hostel mode, and Ascend Plus preview

## Prototype limitations
Meals, macros, and rupee costs are sample data, not verified nutrition analysis or live market pricing. Data is held in memory and resets when the app process restarts. Authentication, backend persistence, calendar notifications, expert marketplace, moderation, and payment processing are not yet implemented. Nutrition targets are not medical advice.

## Next architecture steps
Introduce a backend API and persistent data layer, user authentication, verified recipe/macronutrient records, region-specific price sourcing, expert credential review, and compliant subscription/payment integrations.
