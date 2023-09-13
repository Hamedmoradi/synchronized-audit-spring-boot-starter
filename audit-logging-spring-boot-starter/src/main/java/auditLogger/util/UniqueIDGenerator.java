package auditLogger.util;

import org.slf4j.MDC;

import javax.servlet.http.HttpServletRequest;

public class UniqueIDGenerator {

    private static final String REQUEST_ID_HEADER_NAME = "X-Request-ID";
    private static final String CORRELATION_ID_HEADER_NAME = "X-Correlation-ID";

    public void generateAndSetMDC(HttpServletRequest request) {
        MDC.clear();
        String requestId = request.getHeader(REQUEST_ID_HEADER_NAME);
        if (requestId == null)
            requestId = request.getHeader("traceId");
        MDC.put(REQUEST_ID_HEADER_NAME, requestId);

        String correlationId = request.getHeader(CORRELATION_ID_HEADER_NAME);
        if (correlationId == null)
            correlationId = request.getHeader("traceId");
        MDC.put(CORRELATION_ID_HEADER_NAME, correlationId);
    }

}
