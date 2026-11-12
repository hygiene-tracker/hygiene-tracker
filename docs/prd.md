# Product Requirements Document (PRD): Hygiene Tracker

**Team:** Team 1 **Repo:** [hygiene-tracker/hygiene-tracker](https://github.com/hygiene-tracker/hygiene-tracker) · **Last updated:** 2026-11-02

---

## 1. Users

- **Primary user:** Stinky people like STEM majors and Punk Rockers who need to improve their hygiene
- **Admin:** moderates the leaderboard.

## 2. Problem

STEM majors and Punk Rockers stereotypically are stinky. General habit trackers are
private and not built for hygiene, so there is no existing app for this.

## 3. Success measures

| # | Measure | Target | How we check it |
|---|---|---|---|
| S1 | Users keep a shower streak | A user who showers daily for 7 days sees a streak of 7 and a score of 70 | Automated test on seeded data in CI |
| S2 | The leaderboard ranks users correctly | With 12 seeded users, the top 10 and the loser are correct every time | Integration test in CI |
| S3 | The MVP works for users on both platforms | 7 of 7 MVP stories work on web and Android against production | Planned-versus-shipped slide at the final presentation |

## 4. Scope: MVP, vertical slice, and not building

- **MVP stories:** PRD-1 to PRD-7 (account baseline: PRD-1 to PRD-3)
- **Vertical slice:** PRD-4, log a shower and see the streak and score update
- **Not building (this term):** hygiene types other than showers until showers are complete;
  routines or schedules (shower every x days, shave every y days); profile pages beyond your own
  stats, a display name and a preset avatar; uploading your own profile picture; editing or
  deleting a logged shower

## 5. User stories and acceptance criteria

| ID | Story | Acceptance criteria |
|---|---|---|
| PRD-1 | As a user, I want to sign in with GitHub or Google so that I don't need a new password. | **Given** I am signed out, **when** I complete GitHub or Google sign-in on web or Android, **then** I see my stats screen. **Given** my verified email is registered through GitHub, **when** I sign in with Google, **then** I see "This email is registered with GitHub" and no new account is created. |
| PRD-2 | As a user, I want to set a display name and preset avatar so that the leaderboard does not show my real name. | **Given** I am signed in, **when** I save a display name and avatar, **then** the leaderboard shows them and never my email. **Given** an empty display name, **when** I save, **then** I see "Display name is required". |
| PRD-3 | As a user, I want to delete my account so that my data is removed. | **Given** I have logged showers, **when** I delete my account, **then** I am signed out, my showers are deleted and I am not on the leaderboard. |
| PRD-4 | As a user, I want to log a shower with its rough duration and whether I used shampoo, conditioner, body wash or exfoliant so that it counts toward my streak. | **Given** my streak is 2 and I have not showered today, **when** I log a 10-minute shower with shampoo checked, **then** my streak shows 3, my score rises by 10 and the shower is in my history. **Given** a duration of 0 or over 120 minutes, **when** I submit, **then** I see "Duration must be 1–120 minutes" and nothing is logged. **Given** another user's shower, **when** I request it, **then** I get "Not found". |
| PRD-5 | As a user, I want my score to reward showering and penalize skipping. | **Given** I showered once today, **when** I log a second and then a third shower, **then** my score rises by 5 and then by 0. **Given** my last shower was 3 days ago, **when** I open my stats, **then** I see "3 days since last shower" and 6 points lost (−2, −4) for the two missed days. |
| PRD-6 | As a user, I want a leaderboard of the top 10 and the lowest-scoring active user. | **Given** 12 users who showered in the last 7 days, **when** I open the leaderboard, **then** I see the 10 highest 30-day scores in order and the lowest as "Loser". A user with no shower in 7 days is never the loser. **Given** a user logged more than 10 showers in one day, **when** I open the leaderboard, **then** they are not on it. |
| PRD-7 | As an admin, I want to ban and unban users and revoke the admin role. | **Given** I am an admin, **when** I ban a user, **then** they disappear from the leaderboard, and **when** I unban them, **then** they reappear. **Given** an admin whose role I revoked, **when** they call an admin action, **then** they get 403 and nothing changes. |

### Scoring rules (PRD-5)

- Each day's 1st shower earns 10 points, the 2nd earns 5, and the 3rd and later earn nothing.
- A day with no shower is a missed day, except today (you can still shower) and days before your
  first shower. The k-th missed day in a row costs 2k points (−2, −4, −6, ...), with no cap.
- The score covers the last 30 days including today, the same window as the leaderboard, and can
  go below zero.
- The streak counts consecutive days with a shower, ending today, or ending yesterday if you have
  not showered yet today.

---

### Change log

| Date | PR | What changed and why |
|---|---|---|
| 2026-11-02 | `#<n>` | Initial version |
| 2026-11-11 | #93 | Added the PRD-5 scoring rules, agreed by the team, so the score calculation has a written spec (#92) |
