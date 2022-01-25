package ir.bmi.audit.client.infra.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * hamedMoradi.mailsbox@gmail.com
 */
public class HttpHeaderUtil {
    public static final int FIRST_INDEX = 0;

    public static Map<String, Object> convertHeadersToMap(HttpServletRequest request, String... filteredNames){
        if (request == null) {
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<>();

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();

            if(includeHeaderInMap(name, filteredNames)) {
                Enumeration<String> headersEnumeration = request.getHeaders(name);

                List<String> headers = new ArrayList<>();
                while (headersEnumeration.hasMoreElements()) {
                    headers.add(headersEnumeration.nextElement());
                }

                if (headers.size() > 1) {
                    result.put(name, headers);
                } else {
                    result.put(name, headers.get(FIRST_INDEX));
                }
            }
        }

        return result;
    }

    public static Map<String, Object> convertHeadersToMap(HttpServletResponse response, String... filteredNames){
        if (response == null) {
            return null;
        }

        Collection<String> headerNames = response.getHeaderNames();

        Map<String, Object> result = new LinkedHashMap<>();

        for (String name : headerNames) {

            if(includeHeaderInMap(name, filteredNames)) {
                Collection<String> headers = response.getHeaders(name);

                if (headers.size() > 1) {
                    result.put(name, new ArrayList<>(response.getHeaders(name)));
                } else {
                    result.put(name, response.getHeader(name));
                }
            }
        }

        return result;
    }

    private static boolean includeHeaderInMap(String headerName, String... filteredNames) {
        for (String filterName : filteredNames) {
            if (headerName.equalsIgnoreCase(filterName)) {
                return true;
            }
        }

        return false;
    }
}
