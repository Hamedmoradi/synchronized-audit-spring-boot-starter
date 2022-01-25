package ir.bmi.audit.client.masking.replaceable;

import ir.bmi.audit.client.masking.finder.ValueFinder;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public abstract class ReplaceableFinder extends ValueFinder<ReplaceableFinderResult> {
    public ReplaceableFinder(String[] values) {
        super(values);
    }
}
