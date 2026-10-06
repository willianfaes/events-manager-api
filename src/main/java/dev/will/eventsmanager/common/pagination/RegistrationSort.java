package dev.will.eventsmanager.common.pagination;

import org.springframework.data.domain.Sort;

public enum RegistrationSort {
    NEWEST("registeredAt", Sort.Direction.DESC),
    OLDEST("registeredAt", Sort.Direction.ASC),
    EVENT_DATE_ASC("event.schedule", Sort.Direction.ASC),
    EVENT_DATE_DESC("event.schedule", Sort.Direction.DESC),
    EVENT_NAME_ASC("event.name", Sort.Direction.ASC);

    private final String property;
    private final Sort.Direction direction;

    RegistrationSort(String property, Sort.Direction direction) {
        this.property = property;
        this.direction = direction;
    }

    public Sort toSort() {
        return Sort.by(direction, property);
    }
}
