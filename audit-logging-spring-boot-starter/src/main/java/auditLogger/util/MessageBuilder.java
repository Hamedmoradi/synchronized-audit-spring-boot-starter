package auditLogger.util;

import auditLogger.model.Message;
import auditLogger.model.ResponseInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import auditLogger.model.RequestInfo;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;

@Slf4j
public class MessageBuilder {

    public static String getRequestMessage(RequestInfo requestInfo, HttpServletRequest request) throws JsonProcessingException {
        Message message = new Message(requestInfo, "auditRequest", request.getHeader(HttpHeaders.AUTHORIZATION));
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(message);
        json.replaceAll(".*\": null(,)?\\r\\n", "");
        log.info(json);
        return json;
    }
    public static String getResponseMessage(ResponseInfo responseInfo, HttpServletRequest request) throws JsonProcessingException {
        Message message = new Message(responseInfo, "auditResponse", request.getHeader(HttpHeaders.AUTHORIZATION));
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(message);
        json.replaceAll(".*\": null(,)?\\r\\n", "");
        log.debug(json);
        return json;
    }

    public static String getType(String json) {
        JSONObject jObject = new JSONObject(json);
        String type = jObject.get("type").toString();
        return type;
    }

    public static String getToken(String json) {
        JSONObject jObject = new JSONObject(json);
        String tokenPart = jObject.get("token").toString();
        return tokenPart;
    }
    public static String separateMessage(String json) {
        JSONObject jObject = new JSONObject(json);
        JSONObject messagePart = jObject.getJSONObject("message");
        return String.valueOf(messagePart);
    }


}
