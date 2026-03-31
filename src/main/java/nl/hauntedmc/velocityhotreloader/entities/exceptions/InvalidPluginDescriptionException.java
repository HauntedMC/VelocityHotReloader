package nl.hauntedmc.velocityhotreloader.entities.exceptions;

public class InvalidPluginDescriptionException extends RuntimeException {

    public InvalidPluginDescriptionException() {
        super();
    }

    public InvalidPluginDescriptionException(String message) {
        super(message);
    }

    public InvalidPluginDescriptionException(Throwable cause) {
        super(cause);
    }
}
