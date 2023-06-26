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
public class ReceiveDigitsAction extends Action<ReceiveDigitsAction> implements ReceivedDigits {

    // We will need to push our ID because we can be called anytime in the flow
    public final static String RECEIVE_DIGITS_ID = "RecvDigitsID";
    
    

    protected Action digitsRecevedAction;
    
    protected ParticipantTag participantTag;
    protected String inputDigitsRegex;
    protected Integer inBetweenDigitsDurationInMilliseconds;
    protected Integer flushDigitsDurationInMilliseconds;

    
    @Override
    public Action clone(SMARequest event) throws CloneNotSupportedException {
        var clone = super.clone(event);
        clone.transactionAttributes.put(RECEIVE_DIGITS_ID, getId().toString());
        return clone;
    }
    
    @Override
    protected StringBuilder getDebugSummary() {
        return super.getDebugSummary()
                .append(" [").append(getInputDigitsRegex()).append(']');
    }
    
    @Override
    public String getReceivedDigits() {
       return getRecievedDigitsFromAction();
    }
    
    @Override
    public ResponseAction getResponse() {
        final var params = ResponseReceiveDigits.Parameters.builder()
                .withCallId(callId)
                .withParticipantTag(participantTag)
                .withInputDigitsRegex(inputDigitsRegex)
                .withInBetweenDigitsDurationInMilliseconds(inBetweenDigitsDurationInMilliseconds)
                .withFlushDigitsDurationInMilliseconds(flushDigitsDurationInMilliseconds)
                .build();
        return ResponseReceiveDigits.builder().withParameters(params).build();
    }

     public static ReceiveDigitsActionBuilder builder() {
        return new ReceiveDigitsActionBuilder();
    }
    
    @NoArgsConstructor
    public static class ReceiveDigitsActionBuilder extends ActionBuilder<ReceiveDigitsActionBuilder, ReceiveDigitsAction> {
        private Action digitsRecevedAction;
        private ParticipantTag participantTag;
        private String inputDigitsRegex;
        private Integer inBetweenDigitsDurationInMilliseconds;
        private Integer flushDigitsDurationInMilliseconds;

        public ReceiveDigitsActionBuilder withDigitsRecevedAction(Action action) {
            this.digitsRecevedAction = action;
            return this;
        }
        
        public ReceiveDigitsActionBuilder withParticipantTag(ParticipantTag value) {
            this.participantTag = value;
            return this;
        }

        public ReceiveDigitsActionBuilder withInputDigitsRegex(String value) {
            this.inputDigitsRegex = value;
            return this;
        }

        public ReceiveDigitsActionBuilder withInBetweenDigitsDurationInMilliseconds(Integer value) {
            this.inBetweenDigitsDurationInMilliseconds = value;
            return this;
        }

        public ReceiveDigitsActionBuilder withFlushDigitsDurationInMilliseconds(Integer value) {
            this.flushDigitsDurationInMilliseconds = value;
            return this;
        }
        
        @Override
        protected ReceiveDigitsAction buildImpl() {
            return new ReceiveDigitsAction(digitsRecevedAction, participantTag, inputDigitsRegex, inBetweenDigitsDurationInMilliseconds, flushDigitsDurationInMilliseconds);
        }

    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.ReceiveDigits;
    }

}
