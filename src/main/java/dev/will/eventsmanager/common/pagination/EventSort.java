package dev.will.eventsmanager.common.pagination;

import org.springframework.data.domain.Sort;

public enum EventSort {

    DATE_ASC("schedule", Sort.Direction.ASC),
    DATE_DESC("schedule", Sort.Direction.DESC),
    NAME_ASC("name", Sort.Direction.ASC),
    NAME_DESC("name", Sort.Direction.DESC);

    private final String property;
    private final Sort.Direction direction;

    EventSort(String property, Sort.Direction direction) {
        this.property = property;
        this.direction = direction;
    }

    public Sort toSort() {
        return Sort.by(direction, property).and(Sort.by("id"));
    }
}
