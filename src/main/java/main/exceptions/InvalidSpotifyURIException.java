package main.exceptions;

public class InvalidSpotifyURIException extends Exception {
    public InvalidSpotifyURIException() {
        super();
    }

    public InvalidSpotifyURIException(String message) {
        super(message);
    }

    public InvalidSpotifyURIException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidSpotifyURIException(Throwable cause) {
        super(cause);
    }
}
