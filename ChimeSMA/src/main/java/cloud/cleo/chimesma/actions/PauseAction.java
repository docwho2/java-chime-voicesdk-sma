/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 *
 * @author sjensen
 */
@Data
@SuperBuilder(setterPrefix = "with")
public class PauseAction extends Action<PauseAction,ResponsePause> {


    protected ParticipantTag participantTag;
    protected Integer durationInMilliseconds;

    @Override
    protected ResponseAction getResponse() {
        final var params = ResponsePause.Parameters.builder()
                .withCallId(getCallId())
                .withParticipantTag(participantTag)
                .withDurationInMilliseconds(durationInMilliseconds)
                .build();
        return ResponsePause.builder().withParameters(params).build();
    }
    
    @Override
    protected StringBuilder getDebugSummary() {
        final var sb = super.getDebugSummary();
        
        if ( durationInMilliseconds != null ) {
            sb.append(" duration=[").append(getDurationInMilliseconds()).append(']');
        }
       
        return sb;       
    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.Pause;
    }
        

}
