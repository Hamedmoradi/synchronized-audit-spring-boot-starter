package auditLogger.client.masking.finder;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public interface Finder<T extends FinderResult> {
    T find(String input);
}
