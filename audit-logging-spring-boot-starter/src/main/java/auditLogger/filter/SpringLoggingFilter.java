package auditLogger.filter;

import auditLogger.client.config.AuditConfiguration;
import auditLogger.client.config.ConfigurationFactory;
import auditLogger.client.infra.HttpServletRequestCopierWrapper;
import auditLogger.client.infra.HttpServletResponseCopier;
import auditLogger.client.infra.http.HttpHeader;
import auditLogger.client.infra.http.HttpHeaderUtil;
import auditLogger.client.kafka.AuditLogProducer;
import auditLogger.model.RequestInfo;
import auditLogger.model.ResponseInfo;
import auditLogger.util.MessageBuilder;
import auditLogger.util.UniqueIDGenerator;
import auditLogger.wrapper.SpringRequestWrapper;
import auditLogger.wrapper.SpringResponseWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Objects;
import java.util.stream.Stream;


@Slf4j(topic = "[AUDIT_LOGGER]")
@RequiredArgsConstructor
public class SpringLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringLoggingFilter.class);
    private static final String TOPIC = "audit_logger";
    private static InetAddress LocalHostLandAddress;

    static {
        try {
            LocalHostLandAddress = getLocalHostLANAddress();
        } catch (UnknownHostException e) {
            log.error("error in getting LAN address", e);
        }
    }

    private final AuditLogProducer auditLogProducer;
    @Autowired
    ApplicationContext context;
    private UniqueIDGenerator generator;
    private AuditConfiguration configuration = ConfigurationFactory.auditConfigurationAsync();

    public SpringLoggingFilter(UniqueIDGenerator generator, AuditLogProducer auditLogProduceForRequest) {
        this.generator = generator;
        this.auditLogProducer = auditLogProduceForRequest;
    }

    public static String getBody(HttpServletRequest request) throws IOException {
        String body;
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        char[] chars = new char[128];
        int bytesRead;
        while ((bytesRead = reader.read(chars)) > 0) builder.append(chars, 0, bytesRead);

        if (reader != null) reader.close();
        body = builder.toString();
        return body;
    }

    private static InetAddress getLocalHostLANAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }

            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        } catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (Stream.of("/landing", "/api/v1/__config.js", "/api/v1/__config.js").anyMatch(s -> request.getRequestURI().matches(s))) {
            chain.doFilter(request, response);
        } else {
            generator.generateAndSetMDC(request);
            try {
                getHandlerMethod(request);
            } catch (Exception e) {
                LOGGER.trace("************* Cannot get handler method ************* ");
            }
            final SpringRequestWrapper wrappedRequest = new SpringRequestWrapper(request);
            final SpringResponseWrapper wrappedResponse = new SpringResponseWrapper(response);


            RequestInfo info = createRequestInfo(wrappedRequest);

            wrappedResponse.setHeader("X-Request-ID", MDC.get("X-Request-ID"));
            wrappedResponse.setHeader("X-Correlation-ID", MDC.get("X-Correlation-ID"));

            request.setAttribute(RequestInfo.REQUEST_ID, info.Id);
            auditRequest(info, request);
            LOGGER.info("*************Request: method={}, uri={}, payload={}, audit={}", wrappedRequest.getMethod(),
                    wrappedRequest.getRequestURI(), IOUtils.toString(wrappedRequest.getInputStream(),
                            wrappedRequest.getCharacterEncoding()));
            try {
                chain.doFilter(wrappedRequest, wrappedResponse);
            } catch (Exception e) {
                createResponseInfo(wrappedRequest, wrappedResponse);
                throw e;
            }
            createResponseInfo(wrappedRequest, wrappedResponse);
            LOGGER.info("Response({} ms): status={}, payload={}, audit={}", IOUtils.toString(wrappedResponse.getContentAsByteArray(), wrappedResponse.getCharacterEncoding()));
        }
    }

    private void getHandlerMethod(HttpServletRequest request) throws Exception {
        RequestMappingHandlerMapping requestMappingHandlerMapping = (RequestMappingHandlerMapping) context.getBean("requestMappingHandlerMapping");
        HandlerExecutionChain handler = requestMappingHandlerMapping.getHandler(request);
        if (Objects.nonNull(handler)) {
            HandlerMethod handlerHandler = (HandlerMethod) handler.getHandler();
            MDC.put("X-Operation-Name", handlerHandler.getBeanType().getSimpleName() + "." + handlerHandler.getMethod().getName());
        }
    }

    @SneakyThrows
    private RequestInfo createRequestInfo(ServletRequest request) {

        RequestInfo info = new RequestInfo();

        info.Id = ((SpringRequestWrapper) request).getHeader("traceId");
        info.customerId = getCustomerId((HttpServletRequest) request);
        info.headers = HttpHeaderUtil.convertHeadersToMap((HttpServletRequest) request, configuration.getFilteredHeaders().toArray(new String[]{}));

        if (((HttpServletRequest) request).getUserPrincipal() != null) {
            info.username = ((HttpServletRequest) request).getUserPrincipal().getName();
        }

        if (((HttpServletRequest) request).getHeader("X-Forwarded-For") != null) {
            info.xForwardedFor = String.format("%.128s", ((HttpServletRequest) request).getHeader("X-Forwarded-For"));
        }
        if (((HttpServletRequest) request).getRequestURL().toString() != null) {
            info.requestURL = String.format("%.512s", ((HttpServletRequest) request).getRequestURL().toString());
        }

        if (((HttpServletRequest) request).getHeader(HttpHeader.USER_AGENT) != null) {
            info.userAgent = String.format("%.256s", ((HttpServletRequest) request).getHeader(HttpHeader.USER_AGENT));
        }

        if (((HttpServletRequest) request).getHeader(HttpHeader.CONTENT_TYPE) != null) {
            info.contentType = String.format("%.129s", ((HttpServletRequest) request).getHeader(HttpHeader.CONTENT_TYPE));
        }
        HttpServletRequestCopierWrapper requestCopier = new HttpServletRequestCopierWrapper((HttpServletRequest) request, info.Id);
        info.payload = getBody(requestCopier);
        info.payload = configuration.getMaskerByRequest((HttpServletRequest) request).mask(info.payload);
        info.serverAddr = LocalHostLandAddress.getHostName() + "/" + LocalHostLandAddress.getHostAddress() + ":" + request.getServerPort();
        info.queryString = ((HttpServletRequest) request).getQueryString();
        info.date = Calendar.getInstance().getTime();
        info.httpMethod = ((HttpServletRequest) request).getMethod();
        info.remoteHost = request.getRemoteAddr();
        return info;
    }

    private void createResponseInfo(SpringRequestWrapper wrappedRequest, SpringResponseWrapper wrappedResponse) throws IOException {
        String requestId = wrappedRequest.getAttribute(RequestInfo.REQUEST_ID).toString();

        if (wrappedResponse.getCharacterEncoding() == null) {
            wrappedResponse.setCharacterEncoding("UTF-8");
        }
        HttpServletResponseCopier responseCopier = new HttpServletResponseCopier(wrappedResponse);
        byte[] copy = responseCopier.getCopy();

        ResponseInfo responseInfo = new ResponseInfo();
        Principal principal = wrappedRequest.getUserPrincipal();
        if (principal != null) {
            responseInfo.username = principal.getName();
        }

        responseInfo.Id = wrappedRequest.getHeader("traceId");
        responseInfo.requestId = requestId;
        String resBody = new String(copy, StandardCharsets.UTF_8);
        responseInfo.payload = configuration.getMaskerByRequest(wrappedRequest).mask(resBody);
        responseInfo.contentType = responseCopier.getHeader(HttpHeader.CONTENT_TYPE);
        responseInfo.date = Calendar.getInstance().getTime();
        responseInfo.status = wrappedResponse.getStatus();
        responseInfo.headers = wrappedResponse.getAllHeaders();
        auditResponse(responseInfo, wrappedRequest);
    }


    private String getCustomerId(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeader.AUTHORIZATION);
        if (authorization != null && authorization.length() > 7) {
            authorization = authorization.substring(7);
            String[] pieces = authorization.split("\\.");
            if (pieces.length == 3) {
                JsonNode jwtPayload = decodeAndParse(pieces[1]);
                return jwtPayload.get("ssn").asText();
            }
        }
        return null;
    }

    private JsonNode decodeAndParse(String b64String) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = new String(Base64.getDecoder().decode(b64String));
            return mapper.readValue(jsonString, JsonNode.class);
        } catch (IOException e) {
            log.error("error decoding {}", b64String, e);
            throw new RuntimeException("error decoding " + b64String, e);
        }
    }

    private void auditResponse(ResponseInfo responseInfo, ServletRequest request) throws IOException {
        String json = MessageBuilder.getResponseMessage(responseInfo, (HttpServletRequest) request);
        auditLogProducer.sendMessage(json, TOPIC);
        log.debug("audit kafka was sent");
    }

    public void auditRequest(RequestInfo requestInfo, ServletRequest request) throws IOException {
        String json = MessageBuilder.getRequestMessage(requestInfo, (HttpServletRequest) request);
        auditLogProducer.sendMessage(json, TOPIC);
        log.debug("audit kafka was sent");
    }
}
