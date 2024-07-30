package cloud.cleo.chimesma.model;

/**
 *  Types that have just a "error" message set
 * 
 * @author sjensen
 */
public interface ErrorMessage {
    
    // Set on ACTION_FAILED
    public String getError();
    
}
