package main.utils.apis.robertify.imagebuilders;

public class ImageBuilderException extends RuntimeException {
    public ImageBuilderException() {
        super();
    }

    public ImageBuilderException(String message) {
        super(message);
    }

    public ImageBuilderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImageBuilderException(Throwable cause) {
        super(cause);
    }
}
