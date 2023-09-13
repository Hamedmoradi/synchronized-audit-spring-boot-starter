package auditLogger.client.interceptors;

import java.lang.annotation.*;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface AuditLogger {
}
