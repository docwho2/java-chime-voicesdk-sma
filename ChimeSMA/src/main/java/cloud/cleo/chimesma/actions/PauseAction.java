/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author sjensen
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PauseAction extends Action<PauseAction,ResponsePause> {


    protected ParticipantTag participantTag;
    protected Integer durationInMilliseconds;

    @Override
    public ResponseAction getResponse() {
        final var params = ResponsePause.Parameters.builder()
                .withCallId(getCallId())
                .withParticipantTag(participantTag)
                .withDurationInMilliseconds(durationInMilliseconds)
                .build();
        return ResponsePause.builder().withParameters(params).build();
    }

    public static PauseActionBuilder builder() {
        return new PauseActionBuilder();
    }
    
    @Override
    protected StringBuilder getDebugSummary() {
        final var sb = super.getDebugSummary();
        
        if ( durationInMilliseconds != null ) {
            sb.append(" duration=[").append(getDurationInMilliseconds()).append(']');
        }
       
        return sb;       
    }

    @NoArgsConstructor
    public static class PauseActionBuilder extends ActionBuilder<PauseActionBuilder, PauseAction> {

        private ParticipantTag participantTag;
        private Integer durationInMilliseconds;

        public PauseActionBuilder withParticipantTag(ParticipantTag value) {
            this.participantTag = value;
            return this;
        }
        
        public PauseActionBuilder withDurationInMilliseconds(Integer value) {
            this.durationInMilliseconds = value;
            return this;
        }
        
        @Override
        protected PauseAction buildImpl() {
          return new PauseAction(participantTag, durationInMilliseconds);
        }

    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.Pause;
    }
        

}
