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
public class StopCallRecordingAction extends Action<StopCallRecordingAction> {


    @Override
    public ResponseAction getResponse() {
        return ResponseStopCallRecording.builder()
                .withParameters(ResponseStopCallRecording.Parameters.builder().withCallId(callId).build()).build();
    }

    public static StopCallRecordingActionBuilder builder() {
        return new StopCallRecordingActionBuilder();
    }

    @NoArgsConstructor
    public static class StopCallRecordingActionBuilder extends ActionBuilder<StopCallRecordingActionBuilder, StopCallRecordingAction> {

        @Override
        protected StopCallRecordingAction buildImpl() {
            return new StopCallRecordingAction();
        }

    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.StopCallRecording;
    }

}
