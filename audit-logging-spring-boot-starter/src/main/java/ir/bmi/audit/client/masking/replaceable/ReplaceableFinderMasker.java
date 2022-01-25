package ir.bmi.audit.client.masking.replaceable;


import ir.bmi.audit.client.masking.FinderMasker;
import ir.bmi.audit.client.masking.finder.ValueFinder;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public abstract class ReplaceableFinderMasker extends FinderMasker<ReplaceableFinderResult> {

    public ReplaceableFinderMasker(ValueFinder<ReplaceableFinderResult> delegate) {
        super(delegate);
    }
}
