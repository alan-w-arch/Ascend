# Ascend Roadmap

## Milestones

### M1 — Stabilize the build and persistence
- Fix the current KAPT/Room build failure.
- Verify the latest persistence changes.
- Confirm Room migrations and DataStore settings survive app restarts.

**Done when:** A clean build succeeds and existing user data survives upgrades.

### M2 — Complete core meal planning
- Make meal creation, editing, deletion, date assignment, and meal completion reliable.
- Ensure Today and Plan show consistent data.

**Done when:** A user can plan and track meals across multiple dates without losing changes.

### M3 — Finish nutrition and recipe experience
- Persist recipes and budget filters.
- Improve calorie and macro summaries.
- Clearly distinguish sample values from verified nutrition data.

**Done when:** Recipe selections and nutrition totals behave consistently and are clearly labeled.

### M4 — Polish UX and accessibility
- Add loading, empty, and error states.
- Validate forms.
- Improve navigation, text scaling, and screen-size behavior.

**Done when:** All primary flows are understandable and usable without workarounds.

### M5 — Testing and release candidate
- Add unit tests for persistence and calculations.
- Test database migrations.
- Manually verify the app on an emulator and a physical device.

**Done when:** The release build succeeds and the critical-flow checklist passes.

### V1 — Version 1 ready
- Update the README, version number, and release notes.
- Create a signed release build.
- Keep a backup of the signing key.

**Done when:** The app can be installed and shared confidently.

## After Version 1

These are larger, optional milestones—not blockers for a usable local app.

### V1.1 — Real nutrition data
Verified food database, serving sizes, and clearer data sources.

### V2 — Accounts and sync
Backend API, authentication, backup, and cross-device synchronization.

### V3 — Ascend Plus
Subscription, payment processing, expert marketplace, credential review, and moderation.

These additions expand Ascend from a personal fitness app into a platform connecting users with verified nutrition professionals.

## Proposed Feature Areas

| Feature | Scope |
|---|---|
| Nutrition scanner | Users photograph a product's nutrition facts or ingredient label. The scanner evaluates the supplied label—not the brand—and explains whether it fits the user's dietary goals, with caveats for missing information. |
| Verified professional profiles | Dietitians, nutritionists, and fitness trainers apply through a web dashboard and submit credentials for verification before offering paid services. |
| Paid diet plans | Professionals list plans and prices. Ascend retains a 10% commission; the professional receives 90% of sale proceeds. |
| Professional and admin dashboards | Professionals manage profiles, plans, client interactions, and reviews. Admins manage credential verification, platform operations, and moderation. |
| Ratings and feedback | Users can rate and review a professional after receiving a diet plan. |
| In-app plan delivery and communication | Professionals deliver plans through Ascend. Users can ask questions; ongoing consultation can be offered through an optional subscription, while one-time plan purchases remain available. |

## Example Payment Flow

| Item | Amount |
|---|---:|
| Plan price | ₹1,000 |
| Ascend commission (10%) | ₹100 |
| Professional payout (90%) | ₹900 |

The eventual backend will need to handle verified credentials, payments and refunds, payout balances, subscriptions, reviews, and secure user-professional messaging.

## Development Approach

Continue making incremental changes to the existing app as requested, while keeping the broader product direction above in view.
