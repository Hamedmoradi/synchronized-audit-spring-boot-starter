package auditLogger.client.config;

/**
 * hamedMoradi.mailsbox@gmail.com
 */
public class UrlPatternMatcher {
    public static boolean match(String pattern, String url) {
        if (url == null || pattern == null) {
            return false;
        }

        if (pattern.equalsIgnoreCase(url)) {
            return true;
        }

        if (pattern.startsWith("/") && pattern.endsWith("/*") && !pattern.contains("/*/")) {
            if (pattern.length() == 2) {
                return true;
            }
            if (url.startsWith(pattern.substring(0, pattern.length() - 2))) {
                return true;
            }
        }

            if (pattern.startsWith("*.") && url.substring(url.lastIndexOf("."), url.length()).equalsIgnoreCase(pattern.substring(1))) {
                return true;
            }


        if (pattern.contains("/*/")) {
            return follow(pattern, url);
        }

        return false;
    }

    public static boolean follow(String pattern, String url) {
        if (pattern == null || url == null) {
            return false;
        }

        String completeUrl = url;
        if (!url.endsWith("/")) {
            completeUrl = url + "/";
        }

        String completePattern = pattern;
        if (!pattern.endsWith("*") && !pattern.endsWith("/")) {
            completePattern = pattern + "/";
        }

        return followRecursive(completePattern, completeUrl);
    }

    private static boolean followRecursive(String pattern, String url) {
        int nextIndex = pattern.indexOf("/*/");
        if (nextIndex == -1) {
            return true;
        }

        String patternPart = pattern.substring(0, nextIndex + 1);

        if (!url.startsWith(patternPart)) {
            return false;
        }

        String nextPattern = pattern.substring(nextIndex + 2);
        String previousPatternPrefix = pattern.substring(0, nextIndex + 2);
        String nextUrl = "";
        if (nextPattern.contains("/*/")) {
            nextUrl = getNextUrl(url, nextPattern, previousPatternPrefix, "/*/");
        } else if (nextPattern.endsWith("/*")) {
            nextUrl = getNextUrl(url, nextPattern, previousPatternPrefix, "/*");
        } else {
            return url.endsWith(nextPattern);
        }

        if (nextUrl == null) {
            return false;
        } else {
        }

        return followRecursive(nextPattern, nextUrl);
    }

    private static String getNextUrl(String currentUrl, String nextPattern, String previousPrefixPattern, String expression) {
        try {
            int nextPathBeginningIndex = currentUrl.indexOf("/", 1);
            String nextValue = nextPattern.substring(0, nextPattern.indexOf(expression));

            if (currentUrl.substring(nextPathBeginningIndex).startsWith(nextValue)) {
                if(currentUrl.startsWith(previousPrefixPattern.substring(0, previousPrefixPattern.indexOf("/*")) + nextValue)) {
                    return null;
                }

                return currentUrl.substring(nextPathBeginningIndex);
            }

            int nextUrlStartingPosition = currentUrl.indexOf(nextValue);
            if (nextUrlStartingPosition == -1) {
                return null;
            }
            return currentUrl.substring(nextUrlStartingPosition);
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }
}
