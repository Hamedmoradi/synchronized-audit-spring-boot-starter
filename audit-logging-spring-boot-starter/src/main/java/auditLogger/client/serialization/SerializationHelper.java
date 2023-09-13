package auditLogger.client.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public class SerializationHelper {

    private static volatile ObjectMapper mapper = null;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("[AUDIT_LOGGER]");

    public static ObjectMapper getMapper() {
        if (mapper == null) {
            synchronized (SerializationHelper.class) {
                if (mapper == null) {
                    mapper = new ObjectMapper();
                    /*FilterProvider filters = new SimpleFilterProvider().addFilter("sensitiveFilter",
                            SimpleBeanPropertyFilter.serializeAllExcept(AuditInfo.FILTERED_FIELDS));
                    mapper.setFilters(filters);*/
                    return mapper;
                }
            }
        }
        return mapper;
    }

    public static String serialize(Throwable throwable) throws JsonProcessingException {
        try {
            return getMapper().writeValueAsString(throwable);
        } catch (JsonProcessingException e) {
            logger.error("JsonProcessingException:", throwable);
        }

        Map<String, Serializable> map = new LinkedHashMap<>();
        map.put("message", throwable.getMessage());
        map.put("stackTrace", throwable.getStackTrace());
        Map<String, Serializable> immutableMap = Collections.unmodifiableMap(new LinkedHashMap<>(map));

        return getMapper().writeValueAsString(immutableMap);
    }
}
