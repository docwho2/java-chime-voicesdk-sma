package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.*;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 *
 * @author sjensen
 */
@Data
@SuperBuilder(setterPrefix = "with")
public class PauseAction extends Action<PauseAction,ResponsePause> {

    protected Integer durationInMilliseconds;

    @Override
    protected ResponseAction getResponse() {
        final var params = ResponsePause.Parameters.builder()
                .withCallId(getCallId())
                .withDurationInMilliseconds(durationInMilliseconds)
                .build();
        return ResponsePause.builder().withParameters(params).build();
    }
    
    @Override
    protected StringBuilder getDebugSummary() {
        final var sb = super.getDebugSummary();
        
        if ( getDurationInMilliseconds() != null ) {
            sb.append(" duration=[").append(getDurationInMilliseconds()).append(']');
        }
       
        return sb;       
    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.Pause;
    }
        

}
