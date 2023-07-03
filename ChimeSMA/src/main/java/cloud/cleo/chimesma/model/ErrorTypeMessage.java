/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
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
