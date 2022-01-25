package ir.bmi.audit.client.infra;

import org.apache.commons.io.IOUtils;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;

public class HttpServletRequestCopierWrapper extends HttpServletRequestWrapper {
    private String requestId;
    private byte[] cachedBody;

    private ByteArrayOutputStream cachedBytes;

    public HttpServletRequestCopierWrapper(HttpServletRequest request, String requestId) {
        super(request);
        this.requestId = requestId;
    }
    public HttpServletRequestCopierWrapper(HttpServletRequest request) throws IOException {
        super(request);
        InputStream requestInputStream = request.getInputStream();
        this.cachedBody = IOUtils.toByteArray(requestInputStream);

    }
    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (cachedBytes == null)
            cacheInputStream();

        return new CachedServletInputStream();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    @Override
    public String getParameter(String name) {
        return super.getParameter(name);
    }

    @Override
    public AsyncContext getAsyncContext() {
        return super.getAsyncContext();
    }

    private void cacheInputStream() throws IOException {
    /* Cache the inputstream in order to read it multiple times. For
     * convenience, I use apache.commons IOUtils
     */
        cachedBytes = new ByteArrayOutputStream();
        IOUtils.copy(super.getInputStream(), cachedBytes);
    }

    /* An inputstream which reads the cached request body */

    public class CachedServletInputStream extends ServletInputStream {
        private ByteArrayInputStream input;

        public CachedServletInputStream() {
      /* create a new input stream from the cached request body */
            input = new ByteArrayInputStream(cachedBytes.toByteArray());
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setReadListener(ReadListener readListener) {

        }

        @Override
        public int read() throws IOException {
            return input.read();
        }
    }
}
