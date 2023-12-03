/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.*;
import static cloud.cleo.chimesma.model.SMARequest.SMAEventType.DIGITS_RECEIVED;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 *
 * @author sjensen
 */
@Data
@SuperBuilder(setterPrefix = "with")
public class ReceiveDigitsAction extends Action<ReceiveDigitsAction, ResponseReceiveDigits> implements ReceivedDigits {

    // We will need to push our ID because we can be called anytime in the flow
    public final static String RECEIVE_DIGITS_ID = "RecvDigitsID";

    protected Action digitsRecevedAction;

    protected ParticipantTag participantTag;
    protected String inputDigitsRegex;
    protected Integer inBetweenDigitsDurationInMilliseconds;
    protected Integer flushDigitsDurationInMilliseconds;

    @Override
    protected StringBuilder getDebugSummary() {
        return super.getDebugSummary()
                .append(" [").append(getInputDigitsRegex()).append(']');
    }

    @Override
    protected void onActionSuccessful() {
        setTransactionAttribute("LastReceivedDigits", getReceivedDigits());
    }

    /**
     * Override this because this is special case where ID needs to be set at render time, not after success like LexBot
     * for example.
     *
     * @return
     */
    @Override
    public Map<String, Object> getTransactionAttributes() {
        final var attrs = super.getTransactionAttributes();
        attrs.put(RECEIVE_DIGITS_ID, getId().toString());
        return attrs;
    }

    @Override
    public String getReceivedDigits() {
        return getRecievedDigitsFromAction();
    }

    @Override
    protected ResponseAction getResponse() {
        final var params = ResponseReceiveDigits.Parameters.builder()
                .withCallId(getCallId())
                .withParticipantTag(participantTag)
                .withInputDigitsRegex(inputDigitsRegex)
                .withInBetweenDigitsDurationInMilliseconds(inBetweenDigitsDurationInMilliseconds)
                .withFlushDigitsDurationInMilliseconds(flushDigitsDurationInMilliseconds)
                .build();
        return ResponseReceiveDigits.builder().withParameters(params).build();
    }

    @Override
    protected Action getNextRoutingAction() {
        if (getEvent() != null && getEvent().getInvocationEventType().equals(DIGITS_RECEIVED)) {
            log.debug("Received Digits [" + getReceivedDigits() + "]");
            return getDigitsRecevedAction();
        } else {
            return super.getNextRoutingAction();
        }
    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.ReceiveDigits;
    }

}
