package auditLogger.client.masking.finder;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public abstract class ValueFinder<T extends FinderResult> implements Finder<T> {
    protected String[] values;

    public ValueFinder(String[] values){
        this.values = values;
    }
}
