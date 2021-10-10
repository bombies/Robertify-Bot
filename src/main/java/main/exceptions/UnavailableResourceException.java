package main.exceptions;

public class UnavailableResourceException extends Exception {
    public UnavailableResourceException() {
        super();
    }

    public UnavailableResourceException(String message) {
        super(message);
    }

    public UnavailableResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnavailableResourceException(Throwable cause) {
        super(cause);
    }
}
