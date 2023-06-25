/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.ResponseAction;
import cloud.cleo.chimesma.model.ResponseActionType;
import cloud.cleo.chimesma.model.ResponseHangup;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class HangupAction extends Action<HangupAction> {


    protected ParticipantTag participantTag;
    protected Integer sipResponseCode;

    @Override
    public ResponseAction getResponse() {
        final var params = ResponseHangup.Parameters.builder()
                .withCallId(callId)
                .withParticipantTag(participantTag)
                .withSipResponseCode(sipResponseCode)
                .build();
        return ResponseHangup.builder().withParameters(params).build();
    }

    public static HangupActionBuilder builder() {
        return new HangupActionBuilder();
    }

    @NoArgsConstructor
    public static class HangupActionBuilder extends ActionBuilder<HangupActionBuilder, HangupAction> {

        @JsonProperty(value = "ParticipantTag")
        private ParticipantTag participantTag;

        @JsonProperty(value = "sipResponseCode")
        private Integer sipResponseCode;

        public HangupActionBuilder withParticipantTag(ParticipantTag value) {
            this.participantTag = value;
            return this;
        }
        
        public HangupActionBuilder withSipResponseCode(Integer value) {
            this.sipResponseCode = value;
            return this;
        }
        
        @Override
        protected HangupAction buildImpl() {
          return new HangupAction(participantTag, sipResponseCode);
        }

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
