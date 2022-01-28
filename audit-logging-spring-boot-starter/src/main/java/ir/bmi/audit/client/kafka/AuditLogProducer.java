package ir.bmi.audit.client.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class AuditLogProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(String message, String topic)  {
        log.info("$$$ -> Producing message --> {}", message);
        try {
            kafkaTemplate.send( message, topic);
        }catch (RuntimeException runtimeException){
            try {
                Files.write(Paths.get("src/main/resources/kafkaMessageFile.txt"), Collections.singleton(message + System.lineSeparator()), StandardOpenOption.CREATE,StandardOpenOption.APPEND);
                FileWriter myWriter = new FileWriter("filename.txt");
                myWriter.write(message);
                myWriter.close();
                System.out.println("Successfully wrote to the file.");
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }

        }
        }
    }