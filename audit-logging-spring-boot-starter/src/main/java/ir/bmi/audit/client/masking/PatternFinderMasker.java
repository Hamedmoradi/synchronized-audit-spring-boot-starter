package ir.bmi.audit.client.masking;

import ir.bmi.audit.client.masking.finder.PatternFinderResult;
import ir.bmi.audit.client.masking.finder.ValueFinder;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public abstract class PatternFinderMasker extends FinderMasker<PatternFinderResult> {
    public PatternFinderMasker(ValueFinder<PatternFinderResult> delegate) {
        super(delegate);
    }
}
