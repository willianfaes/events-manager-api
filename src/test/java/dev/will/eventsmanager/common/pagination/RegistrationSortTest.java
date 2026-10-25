package dev.will.eventsmanager.common.pagination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

class RegistrationSortTest {

    @ParameterizedTest(name = "{0} should end with id ascending")
    @EnumSource(RegistrationSort.class)
    @DisplayName("toSort - should break ties by id so pages are deterministic")
    void toSort_should_end_with_id_as_tie_breaker(RegistrationSort sort) {
        List<Sort.Order> orders = sort.toSort().toList();

        then(orders).hasSize(2);
        then(orders.getLast()).isEqualTo(Sort.Order.asc("id"));
    }
}
