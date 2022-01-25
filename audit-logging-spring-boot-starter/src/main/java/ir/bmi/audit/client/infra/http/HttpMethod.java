package ir.bmi.audit.client.infra.http;

/**
 * List of HTTP methods
 *
 */
public interface HttpMethod {

    /**
     * HTTP GET method
     */
    public static final String GET="GET";
    /**
     * HTTP POST method
     */
    public static final String POST="POST";
    /**
     * HTTP PUT method
     */
    public static final String PUT="PUT";
    /**
     * HTTP DELETE method
     */
    public static final String DELETE="DELETE";
    /**
     * HTTP HEAD method
     */
    public static final String HEAD="HEAD";
    /**
     * HTTP OPTIONS method
     */
    public static final String OPTIONS="OPTIONS";
}
