/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma;

import software.amazon.awssdk.services.polly.model.VoiceId;

/**
 *
 * @author sjensen
 */
public class ExampleFlow extends AbstractFlow {

    @Override
    protected Action getInitialAction() {
        
        final var hangup = HangupAction.builder()
                .withDescription("This is my last step").build();
        
        final var speak3 = SpeakAction.builder()
                .withText("This is the last part")
                .withVoiceId(VoiceId.EMMA)
                //.withNextAction(hangup)
                .build();
        
        final var speak2 = SpeakAction.builder().
                withText("This is part 2")
                .withNextAction(speak3).build();
        
        
        final var speak =  SpeakAction.builder()
                .withText("Welcome to the new age")
                .withVoiceId(VoiceId.SALLI)
                .withNextAction(speak2).build();
        
        final var start = PlayAudioAction.builder()
                .withKey("main.wav")
                .withNextAction(speak).build();

        
        final var hiddenMenu = SpeakAction.builder()
                .withText("You have reached the special Hidden menu!")
                .withVoiceId(VoiceId.SALLI)
                .withNextAction(start)
                .build();
        
        
        final var digits = ReceiveDigitsAction.builder()
                .withInBetweenDigitsDurationInMilliseconds(700)
                .withFlushDigitsDurationInMilliseconds(2000)
                .withInputDigitsRegex("^\\*\\*$")
                .withNextAction(start)
                .withDigitsRecevedAction(hiddenMenu).build();
        
        
        final var call = ConnectCallAndBridgeAction.builder()
                .withUri("+15052162949")
                .withRingbackToneKey("transfer.wav")
                .build();
        
        final var precall = PlayAudioAction.builder()
                .withKey("main.wav")
                .withNextAction(call).build();
        
        return precall;
    }

    @Override
    protected Action getErrorAction() {
        final var errMsg =  SpeakAction.builder()
                .withText("A system error has occured, please call back and try again")
                .build();
        
       return errMsg;
    }

    @Override
    protected void newCallHandler(Action action) {
        log.info("New Call Handler Code Here");
    }
    
    
    @Override
    protected void hangupHandler(Action action) {
        log.info("Hangup Handler Code Here");
    }

    
    
}
