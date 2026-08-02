package com.swp5.library_management.service;

import tools.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Retrieves bibliographic suggestions from the public Open Library ISBN database. */
@Service
public class BookMetadataService {

    private static final String OPEN_LIBRARY_BOOKS_URL =
            "https://openlibrary.org/api/books?format=json&jscmd=data&bibkeys=ISBN:";

    private final RestClient restClient;

    public BookMetadataService() {
        this.restClient = RestClient.create();
    }

    /**
     * Returns a display-ready metadata record, or an empty map when the ISBN is not found.
     * Only ISBN-10 and ISBN-13 values are sent to the external service.
     */
    public Map<String, String> findByIsbn(String isbn) {
        String normalizedIsbn = normalizeIsbn(isbn);
        if (normalizedIsbn == null) {
            return Map.of();
        }

        try {
            JsonNode response = restClient.get()
                    .uri(OPEN_LIBRARY_BOOKS_URL + normalizedIsbn)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode book = response == null ? null : response.path("ISBN:" + normalizedIsbn);
            if (book == null || book.isMissingNode() || book.isNull()) {
                return Map.of();
            }

            Map<String, String> metadata = new LinkedHashMap<>();
            putIfPresent(metadata, "title", book.path("title").asText(null));
            putIfPresent(metadata, "author", names(book.path("authors"), "name"));
            putIfPresent(metadata, "publisher", names(book.path("publishers"), "name"));
            putIfPresent(metadata, "language", language(book.path("languages")));
            return metadata;
        } catch (RestClientException ex) {
            // An unavailable third-party service must not prevent users entering metadata manually.
            return Map.of();
        }
    }

    private static String normalizeIsbn(String isbn) {
        if (!StringUtils.hasText(isbn)) {
            return null;
        }
        if (!isbn.matches("[0-9Xx -]+")) {
            return null;
        }
        String normalized = isbn.replaceAll("[ -]", "").toUpperCase();
        return normalized.matches("\\d{13}") || normalized.matches("\\d{9}[0-9X]")
                ? normalized : null;
    }

    private static String names(JsonNode values, String field) {
        if (!values.isArray()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (JsonNode value : values) {
            String name = value.path(field).asText(null);
            if (StringUtils.hasText(name)) {
                if (result.length() > 0) result.append(", ");
                result.append(name);
            }
        }
        return result.isEmpty() ? null : result.toString();
    }

    private static String language(JsonNode languages) {
        if (!languages.isArray() || languages.isEmpty()) {
            return null;
        }
        String key = languages.get(0).path("key").asText("");
        String code = key.substring(key.lastIndexOf('/') + 1).toLowerCase();
        return switch (code) {
            case "eng" -> "Tiếng Anh";
            case "vie" -> "Tiếng Việt";
            case "jpn" -> "Tiếng Nhật";
            case "kor" -> "Tiếng Hàn";
            case "chi", "zho" -> "Tiếng Trung";
            default -> StringUtils.hasText(code) ? code.toUpperCase() : null;
        };
    }

    private static void putIfPresent(Map<String, String> metadata, String key, String value) {
        if (StringUtils.hasText(value)) {
            metadata.put(key, value);
        }
    }
}
