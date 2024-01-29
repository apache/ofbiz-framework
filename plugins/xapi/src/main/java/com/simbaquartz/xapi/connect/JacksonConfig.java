package com.simbaquartz.xapi.connect;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.datatype.jsr310.;

import java.text.DateFormat;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * Created by Mandeep on 7/30/2017.
 */
@Provider
public class JacksonConfig implements ContextResolver<ObjectMapper> {
    private ObjectMapper mapper;

    public JacksonConfig() {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        mapper.setDateFormat(new RFC3339DateFormat());
//        mapper.registerModule(new JavaTimeModule());
    }

    /**
     * Set the date format for JSON (de)serialization with Date properties.
     * @param dateFormat Date format
     */
    public void setDateFormat(DateFormat dateFormat) {
        mapper.setDateFormat(dateFormat);
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}