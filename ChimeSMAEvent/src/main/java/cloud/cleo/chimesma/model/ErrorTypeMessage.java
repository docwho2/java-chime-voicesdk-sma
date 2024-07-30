package cloud.cleo.chimesma.model;


/**
 * Interface for Responses that contain ErrorType and ErrorMessage
 * 
 * @author sjensen
 */
public interface ErrorTypeMessage {
    
     // Set on ACTION_FAILED
    public String getErrorType();
    public String getErrorMessage();
}
