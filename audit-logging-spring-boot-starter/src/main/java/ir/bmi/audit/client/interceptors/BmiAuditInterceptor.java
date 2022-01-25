package ir.bmi.audit.client.interceptors;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ir.bmi.audit.client.config.AuditConfiguration;
import ir.bmi.audit.client.config.ConfigurationFactory;
import ir.bmi.audit.model.Message;
import ir.bmi.audit.model.MethodCall;
import ir.bmi.audit.model.RequestInfo;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Calendar;
import java.util.UUID;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

@Aspect
@Component
@EnableAspectJAutoProxy
@Slf4j
public class BmiAuditInterceptor {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    HttpServletRequest httpRequest;
    AuditConfiguration configuration = ConfigurationFactory.auditConfiguration();

    @Around("@annotation(ir.bmi.audit.client.interceptors.BmiAudit)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Exception exception = null;
        Object result = null;

        httpRequest = ServletRequestProducingListener.obtain();
        MethodCall methodCall = new MethodCall();
        methodCall.startDate = Calendar.getInstance().getTime();
        methodCall.Id = UUID.randomUUID().toString();
        Method method = getCurrentMethod(point);
        String methodName = method.getName();
        methodCall.methodName = method.getDeclaringClass().getName() + "/" + methodName;
        Object requestId;
        try {
            requestId = httpRequest == null ? null : httpRequest.getAttribute(RequestInfo.REQUEST_ID);
        } catch (IllegalStateException e) {
            requestId = UUID.randomUUID();
        }
        try {
            result = point.proceed();
        } catch (Exception e) {
            exception = e;
        } finally {
            methodCall.requestId = requestId != null ? requestId.toString() : null;
            methodCall.endDate = Calendar.getInstance().getTime();
            Message message = new Message(methodCall, "auditMethodCall",( httpRequest).getHeader(HttpHeaders.AUTHORIZATION));
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(message);

            log.info("{} param is {}.", methodName, Arrays.toString(point.getArgs()));
            kafkaTemplate.send("bmi_audit", json);
        }
        if (exception != null)
            throw exception;
        else
            return result;
    }

    private Method getCurrentMethod(ProceedingJoinPoint point) throws Exception {
        try {
            Signature sig = point.getSignature();
            MethodSignature msig = (MethodSignature) sig;
            Object target = point.getTarget();
            return target.getClass().getMethod(msig.getName(), msig.getParameterTypes());
        } catch (NoSuchMethodException e) {
            throw new Exception(e);
        }
    }

}
