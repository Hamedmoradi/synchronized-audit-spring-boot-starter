package auditLogger.client.infra;

import java.io.*;

public class StreamUtils {

    public static <T extends Flushable & Closeable> void flushBuffer(T stream) throws IOException {
        try{
            stream.flush();
        } catch (IOException e) {
            if(!e.getMessage().equals("Stream is closed"))
            {
                throw e;
            }
        }
    }

    public static <T extends Flushable & Closeable> void closeBuffer(T stream) throws IOException {
        try{
            stream.close();
        } catch (IOException e) {
            if(!e.getMessage().equals("Stream is closed"))
            {
                throw e;
            }
        }
    }

    public static <T extends Flushable & Closeable>  void flushAndCloseBuffer(T stream) throws IOException {
        try{
            stream.flush();
            stream.close();
        } catch (IOException e) {
            if(!e.getMessage().equals("Stream is closed"))
            {
                throw e;
            }
        }
    }
}
