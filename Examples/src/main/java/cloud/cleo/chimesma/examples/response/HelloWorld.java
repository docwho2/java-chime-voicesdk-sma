/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.examples.response;

import cloud.cleo.chimesma.model.*;
import cloud.cleo.chimesma.model.ResponseSpeak.Parameters;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.List;

/**
 * Simple Hello World SMA Event Handler Lambda
 * @author sjensen
 */
public class HelloWorld implements RequestHandler<SMARequest, SMAResponse> {

    
    @Override
    public SMAResponse handleRequest(SMARequest req, Context cntxt) {

        // The Call ID for the first participant (the one calling in)
        // You'll need this in response to many actions
        // https://docs.aws.amazon.com/chime-sdk/latest/dg/case-1.html
        final var callId = req.getCallDetails().getParticipants().get(0).getCallId();
        
        switch (req.getInvocationEventType()) {
            case NEW_INBOUND_CALL:
                // Call Speak Action with just the required parameters
                // https://docs.aws.amazon.com/chime-sdk/latest/dg/speak.html
                final var speak = ResponseSpeak.builder()
                        .withParameters(Parameters.builder()
                                .withCallId(callId)
                                .withText("Hello, you have a built a Voice Application!").build()).build();
                
                // Call hangup, with no parameters it automatically just uses LEG-A
                // https://docs.aws.amazon.com/chime-sdk/latest/dg/hangup.html
                final var hangup = ResponseHangup.builder().build();
                 
                // Now simply return the response with our 2 actions above
                // https://docs.aws.amazon.com/chime-sdk/latest/dg/invoke-on-call-leg.html
                return SMAResponse.builder().withActions(List.of(speak,hangup)).build();
            default:
                // Empty action response
                return SMAResponse.builder().build();
        }

    }

}
