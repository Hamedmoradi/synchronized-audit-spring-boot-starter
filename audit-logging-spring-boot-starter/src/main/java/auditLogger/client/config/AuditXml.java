package auditLogger.client.config;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * hamedMoradi.mailsbox@gmail.com
 */
@XmlRootElement(namespace = "audit", name = "configuration")
@XmlAccessorType(XmlAccessType.FIELD)
public class AuditXml {
    @XmlElement(namespace = "audit", name = "server")
    public Server server;

    @XmlElement(namespace = "audit",name = "maskers")
    public Maskers maskers;

    @XmlElement(namespace = "audit",name = "headersFilter")
    public String headersFilter;

    public static class Maskers{
        @XmlElement(namespace = "audit",name = "masker")
        public List<Masker> listOfMaskers = new ArrayList<>();
    }

    public static class Masker{
        @XmlElement(namespace = "audit",name = "masker-class")
        public String text;

        @XmlElement(namespace = "audit",name = "finder-class")
        public String finder;

        @XmlElement(namespace = "audit",name = "fields")
        public String filters;

        @XmlElement(namespace = "audit",name = "url-pattern")
        public String urlPattern;
    }

    public static class Server{
        @XmlElement(namespace = "audit",name = "service-url")
        public String serviceUrl;

        @XmlElement(namespace = "audit",name = "media-type")
        public String mediaType;
    }


}
