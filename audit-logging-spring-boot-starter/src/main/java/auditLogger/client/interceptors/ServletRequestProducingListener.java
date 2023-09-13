package auditLogger.client.interceptors;

import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

@WebListener
public class ServletRequestProducingListener implements ServletRequestListener{
    private static ThreadLocal<ServletRequest> SERVLET_REQUESTS = new ThreadLocal<>();

    @Override
    public void requestInitialized(ServletRequestEvent servletRequestEvent) {
        SERVLET_REQUESTS.set(servletRequestEvent.getServletRequest());
    }

    @Override
    public void requestDestroyed(ServletRequestEvent servletRequestEvent) {
        SERVLET_REQUESTS.remove();
    }

    public static HttpServletRequest obtain(){
        ServletRequest servletRequest = SERVLET_REQUESTS.get();
        if(servletRequest instanceof HttpServletRequest){
            return (HttpServletRequest) servletRequest;
        }

        return null;
    }
}
