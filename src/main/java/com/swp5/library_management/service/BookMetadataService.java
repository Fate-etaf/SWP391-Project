package com.swp5.library_management.service;

import tools.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Retrieves bibliographic suggestions from the public Open Library ISBN database. */
@Service
public class BookMetadataService {

    private static final String OPEN_LIBRARY_BOOKS_URL =
            "https://openlibrary.org/api/books?format=json&jscmd=data&bibkeys=ISBN:";
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");

    private final RestClient restClient;

    public BookMetadataService() {
        this.restClient = RestClient.create();
    }

    /**
     * Returns a display-ready metadata record, or an empty map when the ISBN is not found.
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

            // Extract 4-digit publishYear from publish_date
            String publishDate = book.path("publish_date").asText(null);
            if (StringUtils.hasText(publishDate)) {
                Matcher matcher = YEAR_PATTERN.matcher(publishDate);
                if (matcher.find()) {
                    metadata.put("publishYear", matcher.group(0));
                }
            }


            // Extract description / notes / subtitle
            String subtitle = book.path("subtitle").asText(null);
            String notes = book.path("notes").asText(null);
            StringBuilder desc = new StringBuilder();
            if (StringUtils.hasText(subtitle)) {
                desc.append(subtitle);
            }
            if (StringUtils.hasText(notes)) {
                if (desc.length() > 0) desc.append("\n");
                desc.append(notes);
            }
            if (desc.length() > 0) {
                metadata.put("description", desc.toString());
            }

            return metadata;
        } catch (RestClientException ex) {
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

    private static void putIfPresent(Map<String, String> metadata, String key, String value) {
        if (StringUtils.hasText(value)) {
            metadata.put(key, value);
        }
    }
}
