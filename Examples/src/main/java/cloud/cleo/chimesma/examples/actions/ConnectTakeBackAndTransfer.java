/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.examples.actions;

import cloud.cleo.chimesma.actions.*;


/**
 * This demonstrates using a a Custom Action to track calls and pull them from Connect
 * and move them to another destination (Which could be another Connect number or just external PSTN).
 * 
 * This makes use of the SIP Diversion Header which can be read by the Connect Script.
 * 
 * In a connect flow, instead of using the transfer Step, you would call a Lambda to perform the transfer.
 * 
 * @see CallAndBridgeActionTBTDiversion
 * @author sjensen
 */
public class ConnectTakeBackAndTransfer extends AbstractFlow  {

    @Override
    protected Action getInitialAction() {
       
        /**
         * Use a custom Action that inherits from the base CallAndBridgeAction
         */
        final var connect = CallAndBridgeActionTBTDiversion.builder()
                .withArn("+18004444444") // This would be a phone number that goes to AWS Connect
                .build();
        
        
        
        /**
         * A simple bot definition that has an "Agent" intent that will then trigger a call out to AWS Connect
         */
        final var bot = StartBotConversationAction.builder()
                .withContent("Welcome, how can I can I help today?")
                .withNextActionFunction((a) -> {
                    switch (a.getIntentName()) {
                        case "Quit":
                            return SpeakAction.builder().withText("Thanks for calling, goodbye").build();
                        case "Agent":
                            // Caller needs to speak to Agent, send to Connect Instance
                            return connect;
                        default:
                            // Didn't match any of the intents
                            return getErrorAction();
                    }
                })
                .build();
        
        // Start the flow with the bot
        return bot;
    }

    @Override
    protected Action getErrorAction() {
         return SpeakAction.builder().withText("Sorry you're having troubles, please call again").withNextAction(new HangupAction()).build();
    }

    @Override
    protected void newCallHandler(Action action) {
       
    }

    @Override
    protected void hangupHandler(Action action) {
        
    }
    
}
