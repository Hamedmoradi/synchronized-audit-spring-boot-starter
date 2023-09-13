package auditLogger.client.masking;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public class NoMaskMasker implements Masker {
    @Override
    public String mask(String input) {
        return input;
    }
}
