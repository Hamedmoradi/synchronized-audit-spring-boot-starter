package auditLogger.client.masking.commons;


import auditLogger.client.masking.PatternFinderMasker;
import auditLogger.client.masking.finder.PatternFinderResult;
import auditLogger.client.masking.finder.ValueFinder;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public class JsonValueMasker extends PatternFinderMasker {
    public static final String MASK_CHAR = "****";
    public static final String EMPTY_STRING = "";
    public static final String DOUBLE_QUOTATION = "\"";

    public JsonValueMasker(ValueFinder<PatternFinderResult> delegate) {
        super(delegate);
    }

    @Override
    public String doMask(String input, PatternFinderResult result) {
        return result.getMatcher().replaceAll(("${key}: \"****\""));
    }
}
