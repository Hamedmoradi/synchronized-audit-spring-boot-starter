package ir.bmi.audit.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import ir.bmi.audit.client.infra.HttpServletResponseCopier;
import ir.bmi.audit.model.ResponseInfo;
import ir.bmi.audit.util.MessageBuilder;
import ir.bmi.audit.util.UniqueIDGenerator;
import ir.bmi.audit.wrapper.SpringResponseWrapper;
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
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import ir.bmi.audit.client.kafka.AuditLogProducer;
import ir.bmi.audit.client.config.AuditConfiguration;
import ir.bmi.audit.client.config.ConfigurationFactory;
import ir.bmi.audit.client.infra.HttpServletRequestCopierWrapper;
import ir.bmi.audit.client.infra.http.HttpHeader;
import ir.bmi.audit.client.infra.http.HttpHeaderUtil;
import ir.bmi.audit.model.RequestInfo;
import ir.bmi.audit.wrapper.SpringRequestWrapper;

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
import java.util.*;



@Slf4j(topic = "[BMI-AUDIT]")
@RequiredArgsConstructor
public class SpringLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringLoggingFilter.class);
    private UniqueIDGenerator generator;
    private String ignorePatterns;
    private boolean logHeaders;
    private AuditConfiguration configuration = ConfigurationFactory.auditConfiguration();
    private static InetAddress LocalHostLandAddress;
    private static final String TOPIC = "bmi_audit";
    private final AuditLogProducer auditLogProducer;

    static {
        try {
            LocalHostLandAddress = getLocalHostLANAddress();
        } catch (UnknownHostException e) {
            log.error("error in getting LAN address", e);
        }
    }

    @Autowired
    ApplicationContext context;

    public SpringLoggingFilter(UniqueIDGenerator generator, String ignorePatterns, boolean logHeaders, AuditLogProducer auditLogProduceForRequest) {
        this.generator = generator;
        this.ignorePatterns = ignorePatterns;
        this.logHeaders = logHeaders;
        this.auditLogProducer = auditLogProduceForRequest;
    }

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (ignorePatterns != null && request.getRequestURI().matches(ignorePatterns)) {
            chain.doFilter(request, response);
        } else {
            generator.generateAndSetMDC(request);
            try {
                getHandlerMethod(request);
            } catch (Exception e) {
                LOGGER.trace("*************Cannot get handler method");
            }
            final long startTime = System.currentTimeMillis();
            final SpringRequestWrapper wrappedRequest = new SpringRequestWrapper(request);
            RequestInfo info;
            if (logHeaders) {
                LOGGER.info("*************Request: method={}, uri={}, payload={}, headers={}, audit={}", wrappedRequest.getMethod(),
                        wrappedRequest.getRequestURI(), IOUtils.toString(wrappedRequest.getInputStream(),
                                wrappedRequest.getCharacterEncoding()), wrappedRequest.getAllHeaders());
                info = createRequestInfo(wrappedRequest);
            } else {
                LOGGER.info("*************Request: method={}, uri={}, payload={}, audit={}", wrappedRequest.getMethod(),
                        wrappedRequest.getRequestURI(), IOUtils.toString(wrappedRequest.getInputStream(),
                                wrappedRequest.getCharacterEncoding()));
                info = createRequestInfo(wrappedRequest);
            }

            final SpringResponseWrapper wrappedResponse = new SpringResponseWrapper(response);
            wrappedResponse.setHeader("X-Request-ID", MDC.get("X-Request-ID"));
            wrappedResponse.setHeader("X-Correlation-ID", MDC.get("X-Correlation-ID"));

            if (log.isTraceEnabled())
                log.trace(">> 1.Started Auditing Request #{}", info.Id);
            request.setAttribute(RequestInfo.REQUEST_ID, info.Id);

            if (log.isTraceEnabled())
                log.trace("2.Started Copying Request #{} Stream", info.Id);
            HttpServletRequestCopierWrapper requestCopier = new HttpServletRequestCopierWrapper((HttpServletRequest) request, info.Id);

            if (log.isTraceEnabled()) {
                log.trace("3.Finished Copying Request #{} Stream", info.Id);
                log.trace("4.Started Serializing Request #{} Body", info.Id);
            }
            request.getParameter("param1");
            info.payload = getBody(requestCopier);

            info.payload = configuration.getMaskerByRequest((HttpServletRequest) request).mask(info.payload);
            if (log.isTraceEnabled())
                log.trace("5.Finished Serializing Request #{} Body", info.Id);
            auditRequest(info, request);
            try {
                chain.doFilter(wrappedRequest, wrappedResponse);
            } catch (Exception e) {
                logResponse(startTime, wrappedResponse, wrappedRequest, 500);
                throw e;
            }
            logResponse(startTime, wrappedResponse, wrappedRequest, wrappedResponse.getStatus());
        }
    }

    private void getHandlerMethod(HttpServletRequest request) throws Exception {
        RequestMappingHandlerMapping mappings1 = (RequestMappingHandlerMapping) context.getBean("requestMappingHandlerMapping");
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = mappings1.getHandlerMethods();
        HandlerExecutionChain handler = mappings1.getHandler(request);
        if (Objects.nonNull(handler)) {
            HandlerMethod handler1 = (HandlerMethod) handler.getHandler();
            MDC.put("X-Operation-Name", handler1.getBeanType().getSimpleName() + "." + handler1.getMethod().getName());
        }
    }

    @SneakyThrows
    private RequestInfo createRequestInfo(ServletRequest request) {
        String requestId = UUID.randomUUID().toString();
        RequestInfo info = new RequestInfo();
        HttpServletRequest req = (HttpServletRequest) request;
        Principal principal = req.getUserPrincipal();
        if (principal != null)
            info.username = principal.getName();

        String authorization = req.getHeader(HttpHeader.AUTHORIZATION);
        if (authorization != null && authorization.length() > 7) {
            authorization = authorization.substring(7);
            String[] pieces = authorization.split("\\.");
            // check number of segments
            if (pieces.length == 3) {
                // get JWTClaims JSON object
                JsonNode jwtPayload = decodeAndParse(pieces[1]);
                info.customerId = jwtPayload.get("ssn").asText();
            }
        }
        info.Id = requestId;

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        Map<String, Object> headers = HttpHeaderUtil.convertHeadersToMap(httpRequest, configuration.getFilteredHeaders().toArray(new String[]{}));

        info.headers = headers;

        String xForwardedFor = ((HttpServletRequest) request).getHeader("X-Forwarded-For");
        if (xForwardedFor != null)
            info.xForwardedFor = String.format("%.128s", xForwardedFor);
        String requestUrl = ((HttpServletRequest) request).getRequestURL().toString();
        if (requestUrl != null)
            info.requestURL = String.format("%.512s", requestUrl);

        String uAgent = ((HttpServletRequest) request).getHeader(HttpHeader.USER_AGENT);
        if (uAgent != null)
            info.userAgent = String.format("%.256s", uAgent);

        String contentType = req.getHeader(HttpHeader.CONTENT_TYPE);
        if (contentType != null) {
            info.contentType = String.format("%.129s", contentType);
        }

        info.serverAddr = LocalHostLandAddress.getHostName() + "/" + LocalHostLandAddress.getHostAddress() + ":" + request.getServerPort();
        info.queryString = ((HttpServletRequest) request).getQueryString();
        info.date = Calendar.getInstance().getTime();
        info.httpMethod = ((HttpServletRequest) request).getMethod();
        info.remoteHost = request.getRemoteAddr();
        return info;
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

    public static String getBody(HttpServletRequest request) throws IOException {
        String body;
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
        char[] chars = new char[128];
        int bytesRead;
        while ((bytesRead = reader.read(chars)) > 0)
            builder.append(chars, 0, bytesRead);

        if (reader != null)
            reader.close();
        body = builder.toString();
        return body;
    }

    private static InetAddress getLocalHostLANAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            // Iterate all NICs (network interface cards)...
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // Iterate all IP addresses assigned to each card...
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {

                        if (inetAddr.isSiteLocalAddress()) {
                            // Found non-loopback site-local address. Return it immediately...
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            // Found non-loopback address, but not necessarily site-local.
                            // Store it as a candidate to be returned if site-local address is not subsequently found...
                            candidateAddress = inetAddr;
                            // Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
                            // only the first. For subsequent iterations, candidate will be non-null.
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                // We did not find a site-local address, but we found some other non-loopback address.
                // Server might have a non-site-local address assigned to its NIC (or it might be running
                // IPv6 which deprecates the "site-local" concept).
                // Return this non-loopback candidate address...
                return candidateAddress;
            }
            // At this point, we did not find a non-loopback address.
            // Fall back to returning whatever InetAddress.getLocalHost() returns...
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

    private void logResponse(long startTime, SpringResponseWrapper wrappedResponse, SpringRequestWrapper wrappedRequest, int overriddenStatus) throws IOException {
        final long duration = System.currentTimeMillis() - startTime;
        wrappedResponse.setCharacterEncoding("UTF-8");
        if (logHeaders) {
            LOGGER.info("Response({} ms): status={}, payload={}, headers={}, audit={}", IOUtils.toString(wrappedResponse.getContentAsByteArray(),
                            wrappedResponse.getCharacterEncoding()), wrappedResponse.getAllHeaders());
            createResponseLog(wrappedRequest, wrappedResponse);

        } else {
            createResponseLog(wrappedRequest, wrappedResponse);
            LOGGER.info("Response({} ms): status={}, payload={}, audit={}",
                    IOUtils.toString(wrappedResponse.getContentAsByteArray(), wrappedResponse.getCharacterEncoding()));
        }
    }

    private void createResponseLog(SpringRequestWrapper wrappedRequest, SpringResponseWrapper wrappedResponse) throws IOException {
        String requestId = wrappedRequest.getAttribute(RequestInfo.REQUEST_ID).toString();
        if (log.isTraceEnabled())
            log.trace(">> 1.Started Auditing Response #{}", requestId);

        if (wrappedResponse.getCharacterEncoding() == null) {
            wrappedResponse.setCharacterEncoding("UTF-8"); // Or whatever default. UTF-8 is good for World Domination.
        }
        HttpServletResponseCopier responseCopier =
                new HttpServletResponseCopier((HttpServletResponse) wrappedResponse);

        String resBody;
//        HttpServletRequest req = (HttpServletRequest) wrappedResponse;
        HttpServletRequest req = (HttpServletRequest) wrappedRequest;
        Principal principal = req.getUserPrincipal();
        ResponseInfo responseInfo = new ResponseInfo();
        if (principal != null)
            responseInfo.username = principal.getName();
        responseInfo.Id = UUID.randomUUID().toString();
        responseInfo.requestId = requestId;
//
        if (log.isTraceEnabled())
            log.trace("2.Started Copying Request #{} Stream", requestId);

        byte[] copy = responseCopier.getCopy();

        if (log.isTraceEnabled())
            log.trace("3.Finished Copying Request #{} Stream", requestId);
//
        resBody = new String(copy, StandardCharsets.UTF_8);
        responseInfo.payload = configuration.getMaskerByRequest(req).mask(resBody);
        responseInfo.contentType = responseCopier.getHeader(HttpHeader.CONTENT_TYPE);
        responseInfo.date = Calendar.getInstance().getTime();
        responseInfo.status = responseCopier.getStatus();

        Map<String, Object> headersMap = HttpHeaderUtil.convertHeadersToMap(responseCopier, configuration.getFilteredHeaders().toArray(new String[]{}));

        responseInfo.headers = headersMap;

        if (log.isTraceEnabled())
            log.trace("4.Started Sending Response #{} to Audit Server", requestId);

        auditResponse(responseInfo, req);

        if (log.isTraceEnabled())
            log.trace("5.Finished Auditing Response #{}", requestId);
    }
    private void auditResponse(ResponseInfo responseInfo, ServletRequest request) throws IOException {

        log.info("implement kafka send message");
        String json = MessageBuilder.getResponseMessage(responseInfo, (HttpServletRequest) request);
        auditLogProducer.sendMessage(json, TOPIC);
        log.debug("audit kafka was sent");
    }

    public void auditRequest(RequestInfo requestInfo, ServletRequest request) throws IOException {
        log.trace("6.Started Sending Request #{} to Audit Server", requestInfo.Id);
        log.info("implement kafka send message");
        String json = MessageBuilder.getRequestMessage(requestInfo, (HttpServletRequest) request);
        auditLogProducer.sendMessage(json, TOPIC);
        log.debug("audit kafka was sent");
    }
}
