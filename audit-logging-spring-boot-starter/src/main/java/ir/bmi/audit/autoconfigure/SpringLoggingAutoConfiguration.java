package ir.bmi.audit.autoconfigure;


import ir.bmi.audit.client.kafka.AuditLogProducer;
import ir.bmi.audit.client.kafka.KafkaCustomPartitioner;
import ir.bmi.audit.util.UniqueIDGenerator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import ir.bmi.audit.filter.SpringLoggingFilter;

import java.util.*;

@Configuration
@ConfigurationProperties(prefix = "bmi.audit")
@ConditionalOnClass(SpringLoggingAutoConfiguration.class)
@Data
@RequiredArgsConstructor
public class SpringLoggingAutoConfiguration {

    private String ignorePatterns;
    private boolean logHeaders;
    @Value("${spring.kafka.producer.bootstrap-servers:kafka:9092}")
    private String kafkaServer;
    @Value("${spring.kafka.producer.batch-size:100000}")
    private String batchSize;
    @Value("${spring.kafka.producer.acks:0}")
    private String acks;
    @Value("${spring.kafka.producer.retries:0}")
    private String retries;
    @Value("${spring.kafka.producer.linger.ms:1}")
    private String linger;
    @Value("${spring.kafka.producer.buffer-memory:33554432}")
    private String bufferMemory;
    @Value("${metadata.fetch.timeout.ms:1000")
    private String timeOut;
    @Value("${max.block.ms:6000}")
    private String maxsBlock;
    @Autowired
    private AuditLogProducer auditLogProduceForRequest;

    @Bean
    public UniqueIDGenerator generator() {
        return new UniqueIDGenerator();
    }

    @Bean
    public SpringLoggingFilter loggingFilter() {
        return new SpringLoggingFilter(generator(), ignorePatterns, logHeaders, auditLogProduceForRequest);
    }

    @ConditionalOnMissingBean
    public ProducerFactory<String, String> producerFactoryString() {
        Map<String, Object> configProps = new HashMap<>();

        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
        configProps.put(ProducerConfig.ACKS_CONFIG, acks);
        configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, linger);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, maxsBlock);
        configProps.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, KafkaCustomPartitioner.class);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put("metadata.fetch.timeout.ms", "1000");
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaTemplate<String, String> kafkaTemplateString() {
        return new KafkaTemplate<>(producerFactoryString());
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLogProducer auditLogProducer(KafkaTemplate<String, String> kafkaTemplate) {

        return new AuditLogProducer(kafkaTemplate);
    }

    public String getIgnorePatterns() {
        return ignorePatterns;
    }

    public void setIgnorePatterns(String ignorePatterns) {
        this.ignorePatterns = ignorePatterns;
    }

    public boolean isLogHeaders() {
        return logHeaders;
    }

    public void setLogHeaders(boolean logHeaders) {
        this.logHeaders = logHeaders;
    }
}
