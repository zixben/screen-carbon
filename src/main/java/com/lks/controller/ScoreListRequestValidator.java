package com.lks.controller;

import java.util.Locale;

final class ScoreListRequestValidator {
	static final int MAX_LIMIT = 100;
	static final int MAX_OFFSET = 10_000;
	static final int MIN_YEAR = 1888;
	static final int MAX_YEAR = 2100;
	private static final int MAX_GENRE_ID = 200_000;

	ScoreListFilters validate(int limit, int offset, String country, String genre, String year) {
		return new ScoreListFilters(
				validateLimit(limit),
				validateOffset(offset),
				validateCountry(country),
				validateGenre(genre),
				validateYear(year));
	}

	private int validateLimit(int limit) {
		if (limit < 1 || limit > MAX_LIMIT) {
			throw new IllegalArgumentException("Limit must be between 1 and " + MAX_LIMIT + ".");
		}
		return limit;
	}

	private int validateOffset(int offset) {
		if (offset < 0 || offset > MAX_OFFSET) {
			throw new IllegalArgumentException("Offset must be between 0 and " + MAX_OFFSET + ".");
		}
		return offset;
	}

	private String validateCountry(String country) {
		String normalized = trimToNull(country);
		if (normalized == null) {
			return null;
		}
		normalized = normalized.toUpperCase(Locale.ROOT);
		if (!normalized.matches("[A-Z]{2}")) {
			throw new IllegalArgumentException("Country filter is invalid.");
		}
		return normalized;
	}

	private String validateGenre(String genre) {
		String normalized = trimToNull(genre);
		if (normalized == null) {
			return null;
		}
		int genreId = parsePositiveInteger(normalized, "Genre filter is invalid.");
		if (genreId > MAX_GENRE_ID) {
			throw new IllegalArgumentException("Genre filter is invalid.");
		}
		return String.valueOf(genreId);
	}

	private String validateYear(String year) {
		String normalized = trimToNull(year);
		if (normalized == null) {
			return null;
		}
		int parsedYear = parsePositiveInteger(normalized, "Year filter is invalid.");
		if (parsedYear < MIN_YEAR || parsedYear > MAX_YEAR) {
			throw new IllegalArgumentException("Year filter is invalid.");
		}
		return String.valueOf(parsedYear);
	}

	private int parsePositiveInteger(String value, String message) {
		if (!value.matches("\\d{1,6}")) {
			throw new IllegalArgumentException(message);
		}
		int parsedValue = Integer.parseInt(value);
		if (parsedValue < 1) {
			throw new IllegalArgumentException(message);
		}
		return parsedValue;
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	record ScoreListFilters(int limit, int offset, String country, String genre, String year) {
	}
}
