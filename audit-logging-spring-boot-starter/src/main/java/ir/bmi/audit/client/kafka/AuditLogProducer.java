package ir.bmi.audit.client.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class AuditLogProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(String message, String topic) {
        log.info("$$$ -> Producing message --> {}", message);


        ListenableFuture<SendResult<String, String>> future =
                this.kafkaTemplate.send(topic, message);
        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
            @Override
            public void onSuccess(SendResult<String, String> result) {
                log.info("Message [{}] delivered with offset {}",
                        message,
                        result.getRecordMetadata().offset());
            }

            @Override
            public void onFailure(Throwable ex) {
                log.warn("Unable to deliver message [{}]. {}",
                        message,
                        ex.getMessage());
            }
        });
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}