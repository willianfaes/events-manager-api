package dev.will.eventsmanager.common.pagination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.BDDAssertions.then;

class PageRequestsTest {

    private static final Sort SORT = Sort.by(Sort.Direction.DESC, "schedule");

    @Test
    @DisplayName("of - should keep page, size and sort when values are within limits")
    void of_should_keep_values_when_within_limits() {
        Pageable pageable = PageRequests.of(3, 10, SORT);

        then(pageable.getPageNumber()).isEqualTo(3);
        then(pageable.getPageSize()).isEqualTo(10);
        then(pageable.getSort()).isEqualTo(SORT);
    }

    @ParameterizedTest(name = "page {0} should become {1}")
    @CsvSource({"-1, 0", "-100, 0", "0, 0"})
    @DisplayName("of - should never use a negative page number")
    void of_should_clamp_page_to_zero_when_negative(int requestedPage, int expectedPage) {
        Pageable pageable = PageRequests.of(requestedPage, 20, SORT);

        then(pageable.getPageNumber()).isEqualTo(expectedPage);
    }

    @ParameterizedTest(name = "size {0} should become {1}")
    @CsvSource({"0, 1", "-5, 1", "1, 1", "50, 50", "51, 50", "1000, 50"})
    @DisplayName("of - should keep size between 1 and 50")
    void of_should_clamp_size_between_one_and_fifty(int requestedSize, int expectedSize) {
        Pageable pageable = PageRequests.of(0, requestedSize, SORT);

        then(pageable.getPageSize()).isEqualTo(expectedSize);
    }
}
