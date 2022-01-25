package ir.bmi.audit.client.config;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * hamedMoradi.mailsbox@gmail.com
 */
@XmlRootElement(namespace = "http://www.bmi.ir/xml/ns/audit", name = "configuration")
@XmlAccessorType(XmlAccessType.FIELD)
public class AuditXml {
    @XmlElement(namespace = "http://www.bmi.ir/xml/ns/audit", name = "server")
    public Server server;

    @XmlElement(namespace = "http://www.bmi.ir/xml/ns/audit",name = "maskers")
    public Maskers maskers;

    @XmlElement(namespace = "http://www.bmi.ir/xml/ns/audit",name = "headersFilter")
    public String headersFilter;

    public static class Maskers{
        @XmlElement(namespace = "http://www.bmi.ir/xml/ns/audit",name = "masker")
        public List<Masker> listOfMaskers = new ArrayList<>();
    }

    public static class Masker{
        @XmlElement(namespace = "http://www.bmi.ir/xml/ns/audit",name = "masker-class")
        public String text;

        @XmlElement(namespace = "http://www.bmi.ir/xml/ns/audit",name = "finder-class")
        public String finder;

        @XmlElement(namespace = "http://www.bmi.ir/xml/ns/audit",name = "fields")
        public String filters;

        @XmlElement(namespace = "http://www.bmi.ir/xml/ns/audit",name = "url-pattern")
        public String urlPattern;
    }

    public static class Server{
        @XmlElement(namespace = "http://www.bmi.ir/xml/ns/audit",name = "service-url")
        public String serviceUrl;

        @XmlElement(namespace = "http://www.bmi.ir/xml/ns/audit",name = "media-type")
        public String mediaType;
    }


}
