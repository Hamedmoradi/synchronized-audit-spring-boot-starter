package auditLogger.client.infra;

import java.io.InputStream;

/**
 * hamedMoradi.mailsbox@gmail.com
 */
public class ClassUtils {

    public static InputStream getResourceAsStream(String path){
        return ClassUtils.class.getClassLoader().getResourceAsStream(path);
    }
}
