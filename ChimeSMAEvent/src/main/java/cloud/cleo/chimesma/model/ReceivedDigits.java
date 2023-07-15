/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
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
