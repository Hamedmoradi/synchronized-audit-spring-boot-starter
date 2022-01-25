package ir.bmi.audit.client.exception;

/**
 * hamedMoradi.mailsbox@gmail.com
 */
public class BootstrappingException extends RuntimeException {

    public BootstrappingException(){
        super("bootstrapping exception");
    }

    public BootstrappingException(String message){
        super(message);
    }
}
