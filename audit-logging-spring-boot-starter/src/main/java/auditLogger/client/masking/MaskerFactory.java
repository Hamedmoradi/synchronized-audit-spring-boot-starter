package auditLogger.client.masking;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public class MaskerFactory {
    private static MaskerFactory maskerFactory;

    private static class SingletonHelper{
        private static final MaskerFactory INSTANCE = new MaskerFactory();
    }

    public static MaskerFactory getInstance() {
        return SingletonHelper.INSTANCE;
    }

    public Masker masker(){
        return null;
    }

}
