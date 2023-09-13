package auditLogger.client.masking;

import auditLogger.client.masking.finder.PatternFinderResult;
import auditLogger.client.masking.finder.ValueFinder;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public abstract class PatternFinderMasker extends FinderMasker<PatternFinderResult> {
    public PatternFinderMasker(ValueFinder<PatternFinderResult> delegate) {
        super(delegate);
    }
}
