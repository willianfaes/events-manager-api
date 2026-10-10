package dev.will.eventsmanager.common.pagination;

import org.springframework.data.domain.Sort;

public enum AttendeeSort {

    NEWEST("registeredAt", Sort.Direction.DESC),
    OLDEST("registeredAt", Sort.Direction.ASC),
    NAME_ASC("user.name", Sort.Direction.ASC);

    private final String property;
    private final Sort.Direction direction;

    AttendeeSort(String property, Sort.Direction direction) {
        this.property = property;
        this.direction = direction;
    }

    public Sort toSort() {
        return Sort.by(direction, property).and(Sort.by("id"));
    }
}
