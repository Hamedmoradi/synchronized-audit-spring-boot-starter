package auditLogger.model;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;
import java.util.Map;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class RequestInfo extends MessageAuditType implements Externalizable{

    public static final String REQUEST_ID = "requestId";

    public String username;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    public Date date;

    public String payload;

    public String customerId;

    public String remoteHost;

    public String xForwardedFor;

    public String httpMethod;

    public String requestURL;

    public String queryString;

    public String userAgent;

    public String Id;

    public String serverAddr;

    public Map<String, Object> headers;

    public String contentType;


    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(username == null ? "" : username == null ? "" : username);
        out.writeObject(date);
        out.writeUTF(payload == null ? "" : payload);
        out.writeUTF(customerId == null ? "" : customerId);
        out.writeUTF(remoteHost == null ? "" : remoteHost);
        out.writeUTF(xForwardedFor == null ? "" : xForwardedFor);
        out.writeUTF(httpMethod == null ? "" : httpMethod);
        out.writeUTF(requestURL == null ? "" : requestURL);
        out.writeUTF(queryString == null ? "" : queryString);
        out.writeUTF(userAgent == null ? "" : userAgent);
        out.writeUTF(Id == null ? "" : Id);
        out.writeUTF(serverAddr == null ? "" : serverAddr);
        out.writeObject(headers);
        out.writeUTF(contentType == null ? "" : contentType);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        username = in.readUTF();
        date = (Date) in.readObject();
        payload = in.readUTF();
        customerId = in.readUTF();
        remoteHost = in.readUTF();
        xForwardedFor = in.readUTF();
        httpMethod = in.readUTF();
        requestURL = in.readUTF();
        queryString = in.readUTF();
        userAgent = in.readUTF();
        Id = in.readUTF();
        serverAddr = in.readUTF();
        headers = (Map<String, Object>) in.readObject();
        contentType = in.readUTF();
    }

    @Override
    public String toString() {
        return "{" +
                "username='" + username + '\'' +
                ", date=" + date +
                ", payload='" + payload + '\'' +
                ", customerId='" + customerId + '\'' +
                ", remoteHost='" + remoteHost + '\'' +
                ", xForwardedFor='" + xForwardedFor + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", requestURL='" + requestURL + '\'' +
                ", queryString='" + queryString + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", Id='" + Id + '\'' +
                ", serverAddr='" + serverAddr + '\'' +
                ", headers=" + headers +
                ", contentType='" + contentType + '\'' +
                '}';
    }
}
