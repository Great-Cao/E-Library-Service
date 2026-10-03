package com.example.elibrary.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * SQLite has no native date/time type, so {@link Instant} values are stored as
 * ISO-8601 UTC text. Keeping the conversion explicit avoids dialect-dependent
 * timestamp mappings and makes the stored representation stable and readable.
 */
@Converter(autoApply = true)
public class InstantAttributeConverter implements AttributeConverter<Instant, String> {

    @Override
    public String convertToDatabaseColumn(Instant attribute) {
        return attribute == null ? null : DateTimeFormatter.ISO_INSTANT.format(attribute);
    }

    @Override
    public Instant convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Instant.parse(dbData);
    }
}
