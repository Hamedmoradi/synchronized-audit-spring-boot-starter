package auditLogger.client.config;


import auditLogger.client.exception.BootstrappingException;
import auditLogger.client.infra.ClassUtils;
import auditLogger.client.masking.FinderMasker;
import auditLogger.client.masking.Masker;
import auditLogger.client.masking.NoMaskMasker;
import auditLogger.client.masking.finder.ValueFinder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.ConstructorUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
@Slf4j
public class ConfigurationFactory {
    public static final String XML_CONFIG_NAME = "audit.xml";
    public static final String XML_CONFIG_PATH = XML_CONFIG_NAME;
    
    private static Object lock = new Object();

    public static final Masker DEFAULT_MASKER = new NoMaskMasker();


    private static AuditConfiguration CONFIGURATION_INSTANCE;

    public static AuditConfiguration auditConfiguration() {
        if (CONFIGURATION_INSTANCE != null) {
            return CONFIGURATION_INSTANCE;
        }

        synchronized (lock) {
            if (CONFIGURATION_INSTANCE != null) {
                return CONFIGURATION_INSTANCE;
            }

            Map<String, Masker> maskersPatternMap = new HashMap<>();

            CONFIGURATION_INSTANCE = new AuditConfiguration();

            try {
                AuditXml auditXml = loadAuditXml();
                validateAuditXml(auditXml);

                CONFIGURATION_INSTANCE.setServiceUrl(auditXml.server.serviceUrl);
                CONFIGURATION_INSTANCE.setMediaType(auditXml.server.mediaType);
                CONFIGURATION_INSTANCE.setFilteredHeaders(tokenizeHeadersFilter(auditXml.headersFilter));


                if (auditXml.maskers == null || auditXml.maskers.listOfMaskers.isEmpty()) {
                    log.info("no Masker configured. Default \"[]\" will be used", DEFAULT_MASKER.getClass().getName());
                    CONFIGURATION_INSTANCE.setDefaultMasker(DEFAULT_MASKER);
                } else {

                    Masker masker = null;

                    for (AuditXml.Masker xmlMasker : auditXml.maskers.listOfMaskers) {
                        String maskerClassName = xmlMasker.text.trim();
                        findMaskerAndFilterIt(maskersPatternMap, xmlMasker, maskerClassName);
                    }
                }

                List<String> orderedPatterns = PathMapping.getPatternsByOrderedPrecedence(maskersPatternMap.keySet());

                for (String pattern : orderedPatterns) {
                    CONFIGURATION_INSTANCE.addMasker(pattern, maskersPatternMap.get(pattern));
                }

                //todo set default masker based on config file
                if (CONFIGURATION_INSTANCE.getDefaultMasker() == null) {
                    CONFIGURATION_INSTANCE.setDefaultMasker(new NoMaskMasker());
                }

                return CONFIGURATION_INSTANCE;

            } catch (Exception e) {
                log.error("[Audit Configuration] could not configure audit. (is audit.xml missing?)");
                e.printStackTrace();
            }

            // TODO: 10/21/2017 return null or throw exception??
            throw new BootstrappingException("Could not configure audit.");
        }
    }

    private static void findMaskerAndFilterIt(Map<String, Masker> maskersPatternMap, AuditXml.Masker xmlMasker, String maskerClassName) {
        Masker masker;
        try {
            Class<? extends Masker> clazz = (Class<? extends Masker>) Class.forName(maskerClassName);

            if (FinderMasker.class.isAssignableFrom(clazz)) {

                String filters = xmlMasker.filters;

                String[] valuesArray = new String[]{};

                if (filters != null && !filters.trim().isEmpty()) {
                    String[] values = filters.split(",");

                    valuesArray = (String[]) Array.newInstance(String.class, values.length);

                    for (int i = 0; i < values.length; i++) {
                        Array.set(valuesArray, i, values[i].trim());
                    }
                }

                String finderClassName = xmlMasker.finder.trim();

                Class<? extends ValueFinder<?>> finderClass = (Class<? extends ValueFinder<?>>) Class.forName(finderClassName);

                ValueFinder<?> valueFinder = ConstructorUtils.getMatchingAccessibleConstructor(finderClass, valuesArray.getClass()).newInstance((Object) valuesArray);

                masker = ConstructorUtils.getMatchingAccessibleConstructor(clazz, finderClass).newInstance(valueFinder);
            } else {
                masker = clazz.newInstance();
            }

            maskersPatternMap.put(xmlMasker.urlPattern, masker);


        } catch (ClassNotFoundException e) {
            log.error("Unable to find class for name [{}]", maskerClassName);

            throw new BootstrappingException("error in instantiating maskers");

            // TODO: 10/21/2017 throws exception or fallback to default?
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
            throw new BootstrappingException("error in instantiating maskers");
        }
    }

    private static void validateAuditXml(AuditXml auditXml) {
        if (auditXml == null) {
            throw new BootstrappingException("could not load audit.xml");
        }

        if (auditXml.server == null || auditXml.server.serviceUrl == null) {
            throw new BootstrappingException("serviceUrl is missing in audit.xml");
        }
    }

    private static Collection<String> tokenizeHeadersFilter(String headersFilter) {
        if (headersFilter == null || headersFilter.trim().isEmpty()) {
            return Collections.emptyList();
        }

        StringTokenizer stringTokenizer = new StringTokenizer(headersFilter, ",");

        Collection<String> tokens = new ArrayList<>();
        while (stringTokenizer.hasMoreElements()) {
            tokens.add(stringTokenizer.nextToken().trim());
        }

        return tokens;
    }

    private static AuditXml loadAuditXml() throws JAXBException {
        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(AuditXml.class);

            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

            return (AuditXml) jaxbUnmarshaller.unmarshal(ClassUtils.getResourceAsStream(XML_CONFIG_PATH));
        } catch (JAXBException e) {
            e.printStackTrace();

            throw e;
        }
    }
}
