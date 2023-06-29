/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 *
 * @author sjensen
 */
@Data
@SuperBuilder(setterPrefix = "with")
@NoArgsConstructor
public class HangupAction extends Action<HangupAction,ResponseHangup> {
    
    protected ParticipantTag participantTag;
    protected Integer sipResponseCode;

    @Override
    protected ResponseAction getResponse() {
        final var params = ResponseHangup.Parameters.builder()
                .withCallId(getCallId())
                .withParticipantTag(participantTag)
                .withSipResponseCode(sipResponseCode)
                .build();
        return ResponseHangup.builder().withParameters(params).build();
    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.Hangup;
    }
    
    @Override
    protected boolean isChainable() {
        return false;
    }
    
    @Override
    public Action getNextAction() {
        return null;  // there can never be a next action for Hangup
    }
      

}
