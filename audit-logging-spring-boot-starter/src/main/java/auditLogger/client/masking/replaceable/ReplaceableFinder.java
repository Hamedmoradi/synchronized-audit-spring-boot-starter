package auditLogger.client.masking.replaceable;

import auditLogger.client.masking.finder.ValueFinder;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public abstract class ReplaceableFinder extends ValueFinder<ReplaceableFinderResult> {
    public ReplaceableFinder(String[] values) {
        super(values);
    }
}
