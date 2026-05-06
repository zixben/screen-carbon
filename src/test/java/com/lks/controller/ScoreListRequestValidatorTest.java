package com.lks.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreListRequestValidatorTest {
	private final ScoreListRequestValidator validator = new ScoreListRequestValidator();

	@Test
	void validateAcceptsFrontendDefaultsAndNormalizesFilters() {
		ScoreListRequestValidator.ScoreListFilters filters = validator.validate(20, 40, " gb ", " 10759 ", " 2034 ");

		assertEquals(20, filters.limit());
		assertEquals(40, filters.offset());
		assertEquals("GB", filters.country());
		assertEquals("10759", filters.genre());
		assertEquals("2034", filters.year());
	}

	@Test
	void validateTreatsEmptyOptionalFiltersAsUnset() {
		ScoreListRequestValidator.ScoreListFilters filters = validator.validate(20, 0, "", " ", null);

		assertNull(filters.country());
		assertNull(filters.genre());
		assertNull(filters.year());
	}

	@Test
	void validateRejectsInvalidPagination() {
		assertThrows(IllegalArgumentException.class, () -> validator.validate(0, 0, null, null, null));
		assertThrows(IllegalArgumentException.class,
				() -> validator.validate(ScoreListRequestValidator.MAX_LIMIT + 1, 0, null, null, null));
		assertThrows(IllegalArgumentException.class, () -> validator.validate(20, -1, null, null, null));
		assertThrows(IllegalArgumentException.class,
				() -> validator.validate(20, ScoreListRequestValidator.MAX_OFFSET + 1, null, null, null));
	}

	@Test
	void validateRejectsMalformedFilters() {
		assertThrows(IllegalArgumentException.class, () -> validator.validate(20, 0, "United Kingdom", null, null));
		assertThrows(IllegalArgumentException.class, () -> validator.validate(20, 0, null, "abc", null));
		assertThrows(IllegalArgumentException.class, () -> validator.validate(20, 0, null, "0", null));
		assertThrows(IllegalArgumentException.class, () -> validator.validate(20, 0, null, "200001", null));
		assertThrows(IllegalArgumentException.class, () -> validator.validate(20, 0, null, null, "abcd"));
		assertThrows(IllegalArgumentException.class,
				() -> validator.validate(20, 0, null, null, String.valueOf(ScoreListRequestValidator.MIN_YEAR - 1)));
		assertThrows(IllegalArgumentException.class,
				() -> validator.validate(20, 0, null, null, String.valueOf(ScoreListRequestValidator.MAX_YEAR + 1)));
	}
}
