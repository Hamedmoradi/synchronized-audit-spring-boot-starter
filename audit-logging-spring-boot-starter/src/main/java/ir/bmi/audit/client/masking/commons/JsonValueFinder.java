package ir.bmi.audit.client.masking.commons;


import ir.bmi.audit.client.masking.finder.PatternFinder;
import ir.bmi.audit.client.masking.finder.PatternFinderResult;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public class JsonValueFinder extends PatternFinder {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(JsonValueFinder.class);

    protected Pattern pattern = null;

    // not a reliable regex pattern in case of complex situations , but works fine for current payloads
    public static final String KEY_VALUE_PATTERN_REGEX_FORMAT = "(?<key>\"" + "%s" + "\"\\s*):\\s*(?<value>[^\\,,^\\[,^\\{,^\\}]+)";
    public static final String JS_IDENTIFIER_REGEX_FORMAT = "(\\w|_|\\$)(\\w|_|\\$|\\d)*";



    public JsonValueFinder(String[] values) {
        super(values);

        if (values == null || values.length == 0) {
            logger.warn("no values are specified to find, finding will have no effect");
            return;
        }

        assertIfIdentifier(values);
        fillPatterns();
    }

    // to prevent regex injection
    private void assertIfIdentifier(String[] values) {
        Pattern pattern = Pattern.compile(JS_IDENTIFIER_REGEX_FORMAT);

        for (String value : values) {
            Matcher matcher = pattern.matcher(value);

            if(!matcher.matches()) {
                throw new IllegalArgumentException(String.format("invalid json key [%s]", value));
            }
        }
    }

    @Override
    public PatternFinderResult find(String input) {
        if (pattern == null) {
            return null;
        }

        Matcher matcher = pattern.matcher(input);

        PatternFinderResult result = new PatternFinderResult();
        result.setMatcher(matcher);

        return result;
    }

    private void fillPatterns() {
        if (this.values != null && this.values.length != 0 ) {
            String prefix = "(";
            String postfix = ")";
            String separator = "|";

            StringBuilder sb = new StringBuilder();
            sb.append(prefix);

            for (int i = 0; i < this.values.length; i++) {
                sb.append(this.values[i]);

                if (i != this.values.length - 1) {
                    sb.append(separator);
                }
            }

            sb.append(postfix);

            pattern = Pattern.compile(String.format(KEY_VALUE_PATTERN_REGEX_FORMAT, sb.toString()));
        }
    }
}
