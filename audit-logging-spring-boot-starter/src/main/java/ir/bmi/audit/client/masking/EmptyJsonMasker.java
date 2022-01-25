package ir.bmi.audit.client.masking;


import ir.bmi.audit.client.masking.Masker;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public class EmptyJsonMasker implements Masker {
    @Override
    public String mask(String input) {
        return "{}";
    }
}
