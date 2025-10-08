package utilities.components;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import play.libs.Json;

import javax.inject.Singleton;

@Singleton
public class JacksonConfig {

    static {
        ObjectMapper mapper = Json.mapper();
        ObjectMapper prettyMapper = Json.mapper();

        // ✅ Register Java 8 time module (LocalDateTime, etc.)
        mapper.registerModule(new JavaTimeModule());
        prettyMapper.registerModule(new JavaTimeModule());

        // ✅ Avoid timestamps (write ISO-8601 instead of numeric)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        prettyMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // ✅ Use snake_case for field mapping
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        prettyMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);


        // Don’t fail on unknown fields (helps with API evolution)
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        prettyMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}

