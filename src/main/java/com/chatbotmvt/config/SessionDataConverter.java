package com.chatbotmvt.config;

import com.chatbotmvt.dto.SessionData;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Converter(autoApply = true)
public class SessionDataConverter implements AttributeConverter<SessionData, String> {
    private final static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(SessionData attribute) {
        try { return objectMapper.writeValueAsString(attribute); }
        catch (Exception e) { return null; }
    }

    @Override
    public SessionData convertToEntityAttribute(String dbData) {
        try { return objectMapper.readValue(dbData, SessionData.class); }
        catch (Exception e) { return new SessionData(); }
    }
}