package auditLogger.client.config;

import auditLogger.client.masking.Masker;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * hamedMoradi.mailsbox@gmail.com
 */
public class AuditConfiguration {
    private String serviceUrl;

    private String mediaType;

    private Collection<String> filteredHeaders;

    private Masker defaultMasker;

    private Map<String, Masker> urlPatternMasker;

    public Collection<String> getFilteredHeaders() {
        return filteredHeaders;
    }

    public void setFilteredHeaders(Collection<String> filteredHeaders) {
        this.filteredHeaders = filteredHeaders;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public Masker getDefaultMasker(){
        return defaultMasker;
    }

    public void setDefaultMasker(Masker defaultMasker) {
        this.defaultMasker = defaultMasker;
    }

    public void addMasker(String pattern, Masker masker){
        if (urlPatternMasker == null) {
            urlPatternMasker = new LinkedHashMap<>();
        }

        urlPatternMasker.put(pattern, masker);
    }

    /**
     *  Get the request and returns the masker associated with this request if the request uri
     *  matches one of the patterns defined in audit.xml.
     *  If the request uri does not match any of the patterns, defaultMasker will be returned
     */
    public Masker getMaskerByRequest(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();

        if(requestUri.endsWith("/")){
            requestUri = requestUri.substring(0, requestUri.length() - 1);
        }

        String uriPathToMatch = requestUri.substring(contextPath.length());

        if (urlPatternMasker == null) {
            return getDefaultMasker();
        }

        for (String pattern : urlPatternMasker.keySet()) {
            if(match(pattern, uriPathToMatch)){
                return urlPatternMasker.get(pattern);
            }
        }

        return getDefaultMasker();
    }

    protected boolean match(String pattern, String url) {
        return UrlPatternMatcher.match(pattern, url);
    }



}
