package com.ne7k.safepaw.walk.service;

import com.ne7k.safepaw.walk.config.WalkProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class CalorieCalculator {

    public static final double DEFAULT_WEIGHT_KG = 10.0;

    private final WalkProperties props;

    /** weightKg × distanceKm × coefficient, 소수 1자리 */
    public double kcal(BigDecimal weightKg, double distanceMeters) {
        double weight = weightKg == null ? DEFAULT_WEIGHT_KG : weightKg.doubleValue();
        double distanceKm = Math.max(0, distanceMeters) / 1000.0;
        double raw = weight * distanceKm * props.calorieCoefficient();
        return Math.round(raw * 10.0) / 10.0;
    }
}