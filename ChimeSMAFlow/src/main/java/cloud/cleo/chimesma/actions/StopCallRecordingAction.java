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
public class StopCallRecordingAction extends Action<StopCallRecordingAction,ResponseStopCallRecording> {


    @Override
    protected ResponseAction getResponse() {
        return ResponseStopCallRecording.builder()
                .withParameters(ResponseStopCallRecording.Parameters.builder().withCallId(getCallId()).build()).build();
    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.StopCallRecording;
    }

}
