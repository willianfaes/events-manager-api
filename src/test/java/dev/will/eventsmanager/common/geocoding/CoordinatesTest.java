package dev.will.eventsmanager.common.geocoding;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class CoordinatesTest {

    private static final Coordinates MILANO = new Coordinates(45.4642, 9.19);
    private static final Coordinates ROMA = new Coordinates(41.9028, 12.4964);

    @Test
    void distanceKmTo_returnsTheStraightLineDistance() {
        then(MILANO.distanceKmTo(ROMA)).isCloseTo(477.0, Offset.offset(5.0));
    }

    @Test
    void distanceKmTo_isZeroForTheSamePoint() {
        then(MILANO.distanceKmTo(MILANO)).isZero();
    }

    @Test
    void distanceKmTo_isTheSameInBothDirections() {
        then(MILANO.distanceKmTo(ROMA)).isCloseTo(ROMA.distanceKmTo(MILANO), Offset.offset(0.001));
    }
}
