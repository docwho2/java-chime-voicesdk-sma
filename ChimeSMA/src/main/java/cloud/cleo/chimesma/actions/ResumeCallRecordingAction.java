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
public class ResumeCallRecordingAction extends Action<ResumeCallRecordingAction,ResponseResumeCallRecording> {


    @Override
    public ResponseAction getResponse() {
        return ResponseResumeCallRecording.builder()
                .withParameters(ResponseResumeCallRecording.Parameters.builder().withCallId(getCallId()).build()).build();
    }

    public static ResumeCallRecordingActionBuilder builder() {
        return new ResumeCallRecordingActionBuilder();
    }

    @NoArgsConstructor
    public static class ResumeCallRecordingActionBuilder extends ActionBuilder<ResumeCallRecordingActionBuilder, ResumeCallRecordingAction> {

        @Override
        protected ResumeCallRecordingAction buildImpl() {
            return new ResumeCallRecordingAction();
        }

    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.ResumeCallRecording;
    }

}
