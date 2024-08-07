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
public class ResumeCallRecordingAction extends Action<ResumeCallRecordingAction,ResponseResumeCallRecording> {


    @Override
    protected ResponseAction getResponse() {
        return ResponseResumeCallRecording.builder()
                .withParameters(ResponseResumeCallRecording.Parameters.builder().withCallId(getCallId()).build()).build();
    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.ResumeCallRecording;
    }

}
