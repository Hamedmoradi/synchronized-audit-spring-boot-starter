package auditLogger.client.masking.replaceable;


import auditLogger.client.masking.FinderMasker;
import auditLogger.client.masking.finder.ValueFinder;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public abstract class ReplaceableFinderMasker extends FinderMasker<ReplaceableFinderResult> {

    public ReplaceableFinderMasker(ValueFinder<ReplaceableFinderResult> delegate) {
        super(delegate);
    }
}
