package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.*;
import java.util.function.Function;
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
    protected Function<HangupAction,Integer> sipResponseCodeF;
    
    @Override
    protected ResponseAction getResponse() {
        final var params = ResponseHangup.Parameters.builder()
                .withCallId(participantTag == null ? getCallId() : null) // If Leg is specified, then don't send callID
                .withParticipantTag(participantTag)
                .withSipResponseCode(getFuncValOrDefault(sipResponseCodeF, sipResponseCode))
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
    protected Action getNextRoutingAction() {
        return null;  // there can never be a next action for Hangup
    }

}
