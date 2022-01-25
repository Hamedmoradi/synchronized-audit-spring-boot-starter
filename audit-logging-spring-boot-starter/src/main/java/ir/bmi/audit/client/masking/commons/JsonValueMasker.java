package ir.bmi.audit.client.masking.commons;


import ir.bmi.audit.client.masking.PatternFinderMasker;
import ir.bmi.audit.client.masking.finder.PatternFinderResult;
import ir.bmi.audit.client.masking.finder.ValueFinder;

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
