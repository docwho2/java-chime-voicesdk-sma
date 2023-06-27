/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

/**
 * Used to simulate an unrecoverable failure, which will be thrown back to AWS
 * 
 * @author sjensen
 */
public class RuntimeFailureException extends RuntimeException {

    public RuntimeFailureException(String message) {
        super(message);
    }
    
    public RuntimeFailureException() {
        super();
    }
    
}
