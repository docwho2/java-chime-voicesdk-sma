/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.examples.actions;

import cloud.cleo.chimesma.actions.*;

/**
 * Simple Hello World SMA Event Handler Lambda using high level Action Objects
 * 
 * @author sjensen
 */
public class HelloWorld extends AbstractFlow {

    /**
     * You implement this method to return the starting point in your flow.
     * 
     * @return an Initial Action 
     */
    @Override
    protected Action getInitialAction() {
       // Call Speak Action with just the required parameters
       // You don't have to specify a call ID as it will default to LEG-A call ID
       // https://docs.aws.amazon.com/chime-sdk/latest/dg/speak.html 
       return SpeakAction.builder()
               .withText("Hello, you have a built a Voice Application!")
               .withNextAction(new HangupAction())
               .build();
    }

    @Override
    protected Action getErrorAction() {
        return null;
    }

    @Override
    protected void newCallHandler(Action action) {
       // Called when a new call comes in, execute any domain specific actions
    }

    @Override
    protected void hangupHandler(Action action) {
        // Called after the Hangup is received, execute any domain specific actions
    }
    
}
