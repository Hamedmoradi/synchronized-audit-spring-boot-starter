package ir.bmi.audit.client.masking;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public abstract class CompositeMasker implements Masker{
    private Masker masker;

    public CompositeMasker(Masker masker){
        this.masker = masker;
    }

    @Override
    public String mask(String input) {
        return masker.mask(input);
    }
}
