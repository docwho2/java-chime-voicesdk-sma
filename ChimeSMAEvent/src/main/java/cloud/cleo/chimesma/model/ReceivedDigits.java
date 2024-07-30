package cloud.cleo.chimesma.model;

/**
 * Applied to Actions that receive Digits
 * 
 * @author sjensen
 */
public interface ReceivedDigits {
    // Where to get the Digits entered from the ActionData
    public final static String RECEIVED_DIGITS = "ReceivedDigits";
    
    public String getReceivedDigits();
}
