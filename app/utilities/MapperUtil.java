package utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

public class MapperUtil {

    public static ObjectMapper projectObjectMapper(){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    public static PGobject toJsonb(Object value) throws SQLException, JsonProcessingException {
        if (value == null) return null;
        PGobject obj = new PGobject();
        obj.setType("jsonb");
        obj.setValue(MapperUtil.projectObjectMapper().writeValueAsString(value));
        return obj;
    }
}
