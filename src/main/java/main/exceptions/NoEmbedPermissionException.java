package main.exceptions;

public class NoEmbedPermissionException extends Exception {
    public NoEmbedPermissionException() {
        super();
    }

    public NoEmbedPermissionException(String message) {
        super(message);
    }

    public NoEmbedPermissionException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoEmbedPermissionException(Throwable cause) {
        super(cause);
    }
}
