package ir.bmi.audit.client.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import java.util.Map;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

public class KafkaCustomPartitioner implements Partitioner {


    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {

        ObjectMapper mapper = new ObjectMapper();
        int partition = 0;
        if (value != null && value.toString().contains("auditRequest")) {
            partition = 0;
        }
        if (value != null && value.toString().contains("auditResponse")) {
            partition = 1;
        }
        if (value != null && value.toString().contains("auditMethodCall")) {
            partition = 2;
        }
        return partition;
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> map) {

    }
}