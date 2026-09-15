package com.ne7k.safepaw.walk.service;

import com.ne7k.safepaw.walk.config.WalkProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CalorieCalculatorTest {

    private final CalorieCalculator calc = new CalorieCalculator(walkProps(1.1));

    @Test
    void formulaIsWeightTimesKmTimesCoefficient() {
        // 8kg · 1.718km · 1.1 ≈ 15.1
        double kcal = calc.kcal(new BigDecimal("8.00"), 1718);
        assertThat(kcal).isCloseTo(15.1, within(0.05));
    }

    @Test
    void nullWeightDefaultsTo10kg() {
        // 10 × 1.0 × 1.1 = 11.0
        assertThat(calc.kcal(null, 1000)).isEqualTo(11.0);
    }

    @Test
    void zeroOrNegativeDistanceIsZero() {
        assertThat(calc.kcal(new BigDecimal("8"), 0)).isEqualTo(0.0);
        assertThat(calc.kcal(new BigDecimal("8"), -50)).isEqualTo(0.0);
    }

    @Test
    void metersAreNotTreatedAsKm() {
        // 실수하면 8 × 1718 × 1.1 ≈ 15118. 서버는 km 환산 후 소수 1자리.
        double kcal = calc.kcal(new BigDecimal("8"), 1718);
        assertThat(kcal).isLessThan(50);
        assertThat(kcal).isGreaterThan(10);
    }

    @Test
    void sameDistanceSameCaloriesRegardlessOfCaller() {
        double live = calc.kcal(new BigDecimal("5.50"), 2400);
        double finish = calc.kcal(new BigDecimal("5.50"), 2400);
        assertThat(live).isEqualTo(finish);
    }

    static WalkProperties walkProps(double coefficient) {
        return new WalkProperties(
                coefficient,
                new WalkProperties.Gps(30, 1.0, 12.0, 100, 10, 5.0),
                new WalkProperties.Batch(60),
                new WalkProperties.Session(180, 2, 24, 50)
        );
    }
}
