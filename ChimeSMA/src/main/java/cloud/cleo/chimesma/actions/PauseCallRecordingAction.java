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
public class PauseCallRecordingAction extends Action<PauseCallRecordingAction,ResponsePauseCallRecording> {


    @Override
    protected ResponseAction getResponse() {
        return ResponsePauseCallRecording.builder()
                .withParameters(ResponsePauseCallRecording.Parameters.builder().withCallId(getCallId()).build()).build();
    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.PauseCallRecording;
    }

}
