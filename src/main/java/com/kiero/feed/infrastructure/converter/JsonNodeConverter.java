package com.kiero.feed.infrastructure.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiero.feed.exception.FeedErrorCode;
import com.kiero.global.exception.KieroException;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class JsonNodeConverter implements AttributeConverter<JsonNode, String> {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(JsonNode attribute) {
		try {
			return attribute == null ? null : objectMapper.writeValueAsString(attribute);
		} catch (Exception e) {
			throw new KieroException(FeedErrorCode.JSON_CONVERT_FAILED);
		}
	}

	@Override
	public JsonNode convertToEntityAttribute(String dbData) {
		try {
			return dbData == null ? null : objectMapper.readTree(dbData);
		} catch (Exception e) {
			throw new KieroException(FeedErrorCode.JSON_CONVERT_FAILED);
		}
	}
}
