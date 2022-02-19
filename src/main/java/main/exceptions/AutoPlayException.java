package main.exceptions;

public class AutoPlayException extends Exception {
    public AutoPlayException() {
        super();
    }

    public AutoPlayException(String message) {
        super(message);
    }

    public AutoPlayException(String message, Throwable cause) {
        super(message, cause);
    }

    public AutoPlayException(Throwable cause) {
        super(cause);
    }
}
