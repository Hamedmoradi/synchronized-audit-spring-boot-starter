package auditLogger.client.config;

import auditLogger.client.exception.BootstrappingException;
import auditLogger.client.masking.FinderMasker;
import auditLogger.client.masking.Masker;
import auditLogger.client.masking.NoMaskMasker;
import auditLogger.client.masking.finder.ValueFinder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.ConstructorUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class ConfigurationFactory {
    private static final Object lock = new Object();
    private static volatile AuditConfiguration CONFIGURATION_INSTANCE;
    public static final Masker DEFAULT_MASKER = new NoMaskMasker();

    // Load AuditXml object asynchronously using CompletableFuture
    private static CompletableFuture<AuditXml> loadAuditXmlAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AuditXml auditXml = new AuditXml();

                // Simulate asynchronous task (e.g., loading from a remote source)
                Thread.sleep(1000);

                // Set values for the server element
                auditXml.server = new AuditXml.Server();
                auditXml.server.serviceUrl = "http://log1:9081/audit/audit";
                auditXml.server.mediaType = "application/json";

                // Set values for the headersFilter element
                auditXml.headersFilter = "Cookie, traceId, X-Nginx-Proxy, Accept, X-Requested-With, X-Forwarded-Proto, X-Forwarded-Host," +
                        "Connection, User-Agent, Referer, Host, Accept-Encoding, requestId, X-Forwarded-For, Accept-Language, X-Real-IP";

                // Set values for the maskers element
                auditXml.maskers = new AuditXml.Maskers();
                auditXml.maskers.listOfMaskers = new ArrayList<>();

                AuditXml.Masker masker1 = new AuditXml.Masker();
                masker1.text = "auditLogger.client.masking.commons.JsonValueMasker";
                masker1.finder = "auditLogger.client.masking.commons.JsonValueFinder";
                masker1.fields = "*";
                masker1.urlPattern = "/*";
                auditXml.maskers.listOfMaskers.add(masker1);

                AuditXml.Masker masker2 = new AuditXml.Masker();
                masker2.text = "auditLogger.client.masking.EmptyJsonMasker";
                masker2.urlPattern = "/*";
                auditXml.maskers.listOfMaskers.add(masker2);

                return auditXml;
            } catch (Exception e) {
                log.error("Error creating AuditXml object", e);
                throw new BootstrappingException("Error creating AuditXml object");
            }
        });
    }

    public static AuditConfiguration auditConfigurationAsync() {
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
                // Use CompletableFuture to load AuditXml asynchronously
                AuditXml auditXml = loadAuditXmlAsync().get();

                validateAuditXml(auditXml);

                CONFIGURATION_INSTANCE.setServiceUrl(auditXml.server.serviceUrl);
                CONFIGURATION_INSTANCE.setMediaType(auditXml.server.mediaType);
                CONFIGURATION_INSTANCE.setFilteredHeaders(tokenizeHeadersFilter(auditXml.headersFilter));

                if (auditXml.maskers == null || auditXml.maskers.listOfMaskers.isEmpty()) {
                    log.info("No Masker configured. Default \"NoMaskMasker\" will be used");
                    CONFIGURATION_INSTANCE.setDefaultMasker(DEFAULT_MASKER);
                } else {
                    for (AuditXml.Masker xmlMasker : auditXml.maskers.listOfMaskers) {
                        String maskerClassName = xmlMasker.text.trim();
                        findMaskerAndFilterIt(maskersPatternMap, xmlMasker, maskerClassName);
                    }
                }

                List<String> orderedPatterns = PathMapping.getPatternsByOrderedPrecedence(maskersPatternMap.keySet());

                for (String pattern : orderedPatterns) {
                    CONFIGURATION_INSTANCE.addMasker(pattern, maskersPatternMap.get(pattern));
                }

                if (CONFIGURATION_INSTANCE.getDefaultMasker() == null) {
                    CONFIGURATION_INSTANCE.setDefaultMasker(new NoMaskMasker());
                }

                return CONFIGURATION_INSTANCE;

            } catch (InterruptedException | ExecutionException e) {
                log.error("[Audit Configuration] could not configure audit asynchronously.", e);
                throw new BootstrappingException("Could not configure audit.");
            }
        }
    }

    private static void findMaskerAndFilterIt(Map<String, Masker> maskersPatternMap, AuditXml.Masker xmlMasker, String maskerClassName) {
        try {
            Class<? extends Masker> clazz = (Class<? extends Masker>) Class.forName(maskerClassName);
            Masker masker;

            if (FinderMasker.class.isAssignableFrom(clazz)) {
                String filters = xmlMasker.filters;

                String[] valuesArray = {};

                if (filters != null && !filters.trim().isEmpty()) {
                    String[] values = filters.split(",");
                    valuesArray = Arrays.copyOf(values, values.length);
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
            throw new BootstrappingException("Error in instantiating maskers");
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            log.error("Error in instantiating maskers", e);
            throw new BootstrappingException("Error in instantiating maskers");
        }
    }

    private static void validateAuditXml(AuditXml auditXml) {
        if (auditXml == null) {
            throw new BootstrappingException("Could not load audit.xml");
        }

        if (auditXml.server == null || auditXml.server.serviceUrl == null) {
            throw new BootstrappingException("serviceUrl is missing in audit.xml");
        }
    }

    private static Collection<String> tokenizeHeadersFilter(String headersFilter) {
        if (headersFilter == null || headersFilter.trim().isEmpty()) {
            return Collections.singletonList("*");
        }

        StringTokenizer stringTokenizer = new StringTokenizer(headersFilter, ",");

        Collection<String> tokens = new ArrayList<>();
        while (stringTokenizer.hasMoreElements()) {
            tokens.add(stringTokenizer.nextToken().trim());
        }
        return tokens;
    }
}
