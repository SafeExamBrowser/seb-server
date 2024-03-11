package ch.ethz.seb.sebserver.webservice;

public class WebserviceInitException extends RuntimeException {

    public WebserviceInitException(final String message) {
        super(message);
    }

    public WebserviceInitException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
