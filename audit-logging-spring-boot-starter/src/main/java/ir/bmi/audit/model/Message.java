package ir.bmi.audit.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;



@Data
@Slf4j
@AllArgsConstructor
public class Message {

    public MessageAuditType message;
    public  String type;
    public String token;

}
