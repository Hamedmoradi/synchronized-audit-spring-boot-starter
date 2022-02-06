package ir.bmi.audit.client.kafka;

import ir.bmi.audit.util.MessageBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

@Slf4j
@RequiredArgsConstructor
@Component
@EnableRetry
public class AuditLogProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final RestTemplate restTemplate = new RestTemplate();

//    @Value("${bmi.audit.consumer.server:consumerServer:8585/consumer/}")
//    private String consumerServer;

    public void sendMessage(final String message, String topic) {
        log.info("$$$ -> Producing message --> {}", message);
        try {
            ListenableFuture<SendResult<String, String>> future = this.kafkaTemplate.send(topic, message);

            future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
                public void onSuccess(SendResult<String, String> result) {
                    AuditLogProducer.log.info("Message [{}] delivered with offset {}", message, result.getRecordMetadata().offset());
                }
                public void onFailure(Throwable ex) {
                    AuditLogProducer.log.warn("Unable to deliver message [{}]. {}", message, ex.getMessage());
                }
            });
        }catch (RuntimeException exception){
            exception.printStackTrace();
            auditApiCall(message, MessageBuilder.getToken(message), "http://localhost:8585/consumer/");
        }
    }



    private void auditApiCall(String message, String token, String url) {
        log.info("****************************** API CALL ******************************");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<String>(message, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }
}

