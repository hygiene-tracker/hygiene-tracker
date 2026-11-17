package com.hygienetracker.api.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/** Plain unit tests: no Spring context, no database. */
class ScoreCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);

    private final ScoreCalculator calculator = new ScoreCalculator();

    private static LocalDate daysAgo(int days) {
        return TODAY.minusDays(days);
    }

    /** One shower a day on each of the given days ago. */
    private static List<LocalDate> showersOn(int... daysAgo) {
        List<LocalDate> days = new ArrayList<>();
        for (int d : daysAgo) {
            days.add(daysAgo(d));
        }
        return days;
    }

    private ScoreSummary calculate(List<LocalDate> showers) {
        return calculator.calculate(showers, TODAY);
    }

    @Nested
    class AcceptanceCriteria {

        @Test
        void secondAndThirdShowerOfTheDayAdd5ThenNothing() {
            int one = calculate(showersOn(0)).score();
            int two = calculate(showersOn(0, 0)).score();
            int three = calculate(showersOn(0, 0, 0)).score();

            assertThat(two - one).isEqualTo(5);
            assertThat(three - two).isZero();
        }

        @Test
        void lastShowerThreeDaysAgoLoses6PointsForTheTwoMissedDays() {
            ScoreSummary stats = calculate(showersOn(3));

            assertThat(stats.daysSinceLastShower()).isEqualTo(3);
            assertThat(stats.pointsLost()).isEqualTo(2 + 4);
            assertThat(stats.score()).isEqualTo(10 - 6);
        }

        @Test
        void oneShowerADayForSevenDaysIsAStreakOf7AndScoreOf70() {
            ScoreSummary stats = calculate(showersOn(0, 1, 2, 3, 4, 5, 6));

            assertThat(stats.streak()).isEqualTo(7);
            assertThat(stats.score()).isEqualTo(70);
            assertThat(stats.pointsLost()).isZero();
        }

        @Test
        void firstShowerTodayExtendsAStreakOf2To3AndAdds10() {
            ScoreSummary before = calculate(showersOn(1, 2));
            ScoreSummary after = calculate(showersOn(0, 1, 2));

            assertThat(before.streak()).isEqualTo(2);
            assertThat(after.streak()).isEqualTo(3);
            assertThat(after.score() - before.score()).isEqualTo(10);
        }

        @Test
        void neverShoweredIsAllZeroWithNoDaysSinceLastShower() {
            ScoreSummary stats = calculate(List.of());

            assertThat(stats).isEqualTo(new ScoreSummary(0, 0, 0, null));
        }
    }

    @Nested
    class Streak {

        @Test
        void countsFromYesterdayWhenThereIsNoShowerYetToday() {
            assertThat(calculate(showersOn(1, 2, 3)).streak()).isEqualTo(3);
        }

        @Test
        void isZeroWhenTheLastShowerWasBeforeYesterday() {
            assertThat(calculate(showersOn(2, 3, 4)).streak()).isZero();
        }

        @Test
        void stopsAtAGap() {
            assertThat(calculate(showersOn(0, 1, 3, 4)).streak()).isEqualTo(2);
        }

        @Test
        void countsDaysNotShowers() {
            assertThat(calculate(showersOn(0, 0, 0, 1)).streak()).isEqualTo(2);
        }

        @Test
        void usesTheFullHistoryBeyondTheScoringWindow() {
            int[] fortyDays = new int[40];
            for (int d = 0; d < 40; d++) {
                fortyDays[d] = d;
            }
            assertThat(calculate(showersOn(fortyDays)).streak()).isEqualTo(40);
        }
    }

    @Nested
    class MissedDays {

        @ParameterizedTest(name = "{0} missed days in a row lose {1} points")
        @CsvSource({"1, 2", "2, 6", "3, 12", "4, 20"})
        void eachMissedDayInARowCostsTwoMoreThanTheLast(int missed, int expectedLost) {
            // Last shower (missed + 1) days ago; today is never a missed day.
            assertThat(calculate(showersOn(missed + 1)).pointsLost()).isEqualTo(expectedLost);
        }

        @Test
        void aShowerResetsThePenaltyRun() {
            // Missed 5 and 4 days ago (-2, -4), showered 3 days ago, missed 2 and 1 days ago (-2, -4).
            assertThat(calculate(showersOn(6, 3)).pointsLost()).isEqualTo(6 + 6);
        }

        @Test
        void todayIsNeverAMissedDay() {
            assertThat(calculate(showersOn(1)).pointsLost()).isZero();
        }

        @Test
        void daysBeforeTheFirstShowerAreNotMissed() {
            ScoreSummary stats = calculate(showersOn(0));

            assertThat(stats.pointsLost()).isZero();
            assertThat(stats.score()).isEqualTo(10);
        }

        @Test
        void scoreCanGoBelowZero() {
            // 10 points, then 9 missed days in a row: 2 + 4 + ... + 18 = 90 lost.
            ScoreSummary stats = calculate(showersOn(10));

            assertThat(stats.pointsLost()).isEqualTo(90);
            assertThat(stats.score()).isEqualTo(-80);
        }
    }

    @Nested
    class ScoringWindow {

        @Test
        void showersOlderThan30DaysEarnNothing() {
            ScoreSummary stats = calculate(showersOn(30, 31, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
                    13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29));

            assertThat(stats.score()).isEqualTo(30 * 10);
        }

        @Test
        void aMissedRunThatStartedBeforeTheWindowKeepsCountingInsideIt() {
            // Last shower 32 days ago: missed days run from 31 days ago to yesterday.
            // Only the last 29 (29 days ago to yesterday) are in the window; they are the 3rd to 31st
            // missed days in a row, costing 2 * (3 + 4 + ... + 31).
            int expected = 0;
            for (int k = 3; k <= 31; k++) {
                expected += 2 * k;
            }
            ScoreSummary stats = calculate(showersOn(32));

            assertThat(stats.pointsLost()).isEqualTo(expected);
            assertThat(stats.score()).isEqualTo(-expected);
            assertThat(stats.daysSinceLastShower()).isEqualTo(32);
        }
    }

    @Nested
    class Input {

        @Test
        void showersCanBeInAnyOrder() {
            assertThat(calculate(showersOn(2, 0, 1))).isEqualTo(calculate(showersOn(0, 1, 2)));
        }

        @Test
        void aGapInTheMiddleIsPenalizedEvenAfterRecentShowers() {
            // Showered 5 days ago, missed 4 and 3 days ago (-2, -4), then 2, 1 and 0 days ago.
            ScoreSummary stats = calculate(showersOn(5, 2, 1, 0));

            assertThat(stats.pointsLost()).isEqualTo(6);
            assertThat(stats.score()).isEqualTo(40 - 6);
            assertThat(stats.streak()).isEqualTo(3);
        }

        @Test
        void rejectsAShowerAfterToday() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> calculator.calculate(List.of(TODAY.plusDays(1)), TODAY))
                    .withMessageContaining("after today");
        }
    }

    @ParameterizedTest(name = "{0} showers in a day earn {1} points")
    @CsvSource({"0, 0", "1, 10", "2, 15", "3, 15", "11, 15"})
    void pointsForDay(int showers, int expected) {
        assertThat(ScoreCalculator.pointsForDay(showers)).isEqualTo(expected);
    }
}
