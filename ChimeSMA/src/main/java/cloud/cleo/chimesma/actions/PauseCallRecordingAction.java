/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author sjensen
 */
@Data
@NoArgsConstructor
public class PauseCallRecordingAction extends Action<PauseCallRecordingAction> {


    @Override
    public ResponseAction getResponse() {
        return ResponsePauseCallRecording.builder()
                .withParameters(ResponsePauseCallRecording.Parameters.builder().withCallId(callId).build()).build();
    }

    public static PauseCallRecordingActionBuilder builder() {
        return new PauseCallRecordingActionBuilder();
    }

    @NoArgsConstructor
    public static class PauseCallRecordingActionBuilder extends ActionBuilder<PauseCallRecordingActionBuilder, PauseCallRecordingAction> {

        @Override
        protected PauseCallRecordingAction buildImpl() {
            return new PauseCallRecordingAction();
        }

    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.PauseCallRecording;
    }

}
