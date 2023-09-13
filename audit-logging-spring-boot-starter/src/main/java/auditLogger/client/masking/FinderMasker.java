package auditLogger.client.masking;


import auditLogger.client.masking.finder.Finder;
import auditLogger.client.masking.finder.FinderResult;
import auditLogger.client.masking.finder.ValueFinder;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public abstract class FinderMasker<T extends FinderResult> implements Masker, Finder<T> {
    protected ValueFinder<T> delegate;

    public FinderMasker(ValueFinder<T> delegate){
        this.delegate = delegate;
    }

    public abstract String doMask(String input, T result);

    @Override
    public T find(String input) {
        return delegate.find(input);
    }

    @Override
    public String mask(String input) {
        T result = find(input);

        if (result == null) {
            return input;
        }

        return doMask(input, result);
    }
}
