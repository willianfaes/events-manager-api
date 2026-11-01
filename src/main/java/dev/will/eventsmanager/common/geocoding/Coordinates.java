package dev.will.eventsmanager.common.geocoding;

public record Coordinates(double latitude, double longitude) {

    public static final double EARTH_RADIUS_KM = 6371.0;

    // Haversine. EventSpecs repeats it in SQL, because a Specification returns only the entity, not a computed value.
    public double distanceKmTo(Coordinates other) {
        double latitudeDelta = Math.toRadians(other.latitude - latitude);
        double longitudeDelta = Math.toRadians(other.longitude - longitude);
        double a = sinSquared(latitudeDelta / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude))
                * sinSquared(longitudeDelta / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    private static double sinSquared(double angle) {
        return Math.sin(angle) * Math.sin(angle);
    }
}
