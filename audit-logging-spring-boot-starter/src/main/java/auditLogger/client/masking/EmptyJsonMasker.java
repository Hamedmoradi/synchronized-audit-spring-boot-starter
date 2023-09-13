package auditLogger.client.masking;


/**
 * hamedMoradi.mailsbox@gmail.com
 */

public class EmptyJsonMasker implements Masker {
    @Override
    public String mask(String input) {
        return "{}";
    }
}
