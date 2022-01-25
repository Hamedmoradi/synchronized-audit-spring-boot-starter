package ir.bmi.audit.client.infra;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class HttpServletResponseCopier extends HttpServletResponseWrapper {

    private ServletOutputStream servletOutputStream;
    private PrintWriter writer;
    private ServletOutputStreamCopier servletOutputStreamCopier;
    private int httpStatus;

    public HttpServletResponseCopier(HttpServletResponse response) throws IOException {
        super(response);
    }

    @Override public void sendError(int sc) throws IOException {
        httpStatus = sc;
        super.sendError(sc);
    }

    @Override public void sendError(int sc, String msg) throws IOException {
        httpStatus = sc;
        super.sendError(sc, msg);
    }

    @Override public void setStatus(int sc) {
        httpStatus = sc;
        super.setStatus(sc);
    }

    public int getStatus() {
        return httpStatus;
    }

    @Override public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IllegalStateException(
                "getWriter() has already been called on this response.");
        }
        if (servletOutputStream == null) {
            servletOutputStream = getResponse().getOutputStream();
            servletOutputStreamCopier = new ServletOutputStreamCopier(servletOutputStream);
        }
        return servletOutputStreamCopier;
    }

    @Override public PrintWriter getWriter() throws IOException {
        if (servletOutputStream != null) {
            throw new IllegalStateException(
                "getOutputStream() has already been called on this response.");
        }
        if (writer == null) {
            servletOutputStreamCopier = new ServletOutputStreamCopier(getResponse().getOutputStream());
            OutputStreamWriter out = new OutputStreamWriter(servletOutputStreamCopier, StandardCharsets.UTF_8);
            writer = new PrintWriter(out, true);
        }
        return writer;
    }

    @Override public void flushBuffer() throws IOException {
        if (writer != null) {
            StreamUtils.flushBuffer(writer);
        } else if (servletOutputStream != null) {
            StreamUtils.flushBuffer(servletOutputStreamCopier);
        }
    }

    public byte[] getCopy() {
        if (servletOutputStreamCopier != null) {
            return servletOutputStreamCopier.getCopy();
        } else {
            return new byte[0];
        }
    }
}
