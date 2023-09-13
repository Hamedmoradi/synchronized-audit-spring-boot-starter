package auditLogger.client.config;

import java.util.*;


public class PathMapping {
    public static List<String> getPatternsByOrderedPrecedence(Collection<String> urlPatterns) {
        Set<String> exactMatcherList = new HashSet<>();
        Set<String> extensionMatcherList = new HashSet<>();
        Set<String> pathMatcherList = new HashSet<>();
        Set<String> complexPathMatcherList = new HashSet<>();

        for (String urlPattern : urlPatterns) {
            if (urlPattern.startsWith("/") && urlPattern.endsWith("/*") && !urlPattern.contains("/*/")) {
                pathMatcherList.add(urlPattern);
            } else if (urlPattern.startsWith("*.")) {
                extensionMatcherList.add(urlPattern);
            } else if (urlPattern.contains("/*/")) {
                complexPathMatcherList.add(urlPattern);
            } else {
                exactMatcherList.add(urlPattern);
            }
        }

        String[] pathMatcherArray = pathMatcherList.toArray(new String[]{});

        for (int i = 0; i < pathMatcherArray.length; i++) {
            for (int j = i; j < pathMatcherArray.length; j++) {
                if (UrlPatternMatcher.match(pathMatcherArray[i], pathMatcherArray[j])) {
                    String temp = pathMatcherArray[i];
                    pathMatcherArray[i] = pathMatcherArray[j];
                    pathMatcherArray[j] = temp;
                }
            }
        }

        List<String> result = new ArrayList<>();
        result.addAll(exactMatcherList);
        result.addAll(complexPathMatcherList);
        result.addAll(Arrays.asList(pathMatcherArray));
        result.addAll(extensionMatcherList);

        return result;
    }
}
