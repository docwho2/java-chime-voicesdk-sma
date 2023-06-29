/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.*;
import java.util.List;
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
public class RecordAudioAction extends Action<RecordAudioAction,ResponseRecordAudio> {

    
    // Key to store the the recording file in the Transaction Attributes
    public final static String RECORD_AUDIO_BUCKET = "RecordAudioBucket";
    public final static String RECORD_AUDIO_KEY = "RecordAudioKey";
    public final static String RECORD_AUDIO_TERMINATOR = "RecordAudioTerm";

    private Integer durationInSeconds;
    private Integer silenceDurationInSeconds;
    private Integer silenceThreshold;
    private List<Character> recordingTerminators;
    private String bucketName = System.getenv("RECORD_BUCKET");
    private String prefix;

    @Override
    public ResponseAction getResponse() {
        
        final var dest = ResponseRecordAudio.RecordingDestination.builder()
                .withBucketName(bucketName)
                .withPrefix(prefix)
                .build();

        final var params = ResponseRecordAudio.Parameters.builder()
                .withCallId(getCallId())
                .withDurationInSeconds(durationInSeconds)
                .withSilenceDurationInSeconds(silenceDurationInSeconds)
                .withSilenceThreshold(silenceThreshold)
                .withRecordingTerminators(recordingTerminators)
                .withRecordingDestination(dest)
                .build();
        return ResponseRecordAudio.builder().withParameters(params).build();
    }

    @Override
    protected boolean isChainable() {
        return false;
    }

    @Override
    protected StringBuilder getDebugSummary() {
        final var sb = super.getDebugSummary();

        if (prefix != null) {
            sb.append(" prefix=[").append(getPrefix()).append(']');
        }

        return sb;
    }

    public static RecordAudioActionBuilder builder() {
        return new RecordAudioActionBuilder();
    }

    @NoArgsConstructor
    public static class RecordAudioActionBuilder extends ActionBuilder<RecordAudioActionBuilder, RecordAudioAction> {

        private Integer durationInSeconds;
        private Integer silenceDurationInSeconds;
        private Integer silenceThreshold;
        private List<Character> recordingTerminators;
        private String bucketName = System.getenv("RECORD_BUCKET");
        private String prefix;

        public RecordAudioActionBuilder withDurationInSeconds(Integer value) {
            this.durationInSeconds = value;
            return this;
        }
        
        public RecordAudioActionBuilder withSilenceDurationInSeconds(Integer value) {
            this.silenceDurationInSeconds = value;
            return this;
        }
        
        public RecordAudioActionBuilder withSilenceThreshold(Integer value) {
            this.silenceThreshold = value;
            return this;
        }

        public RecordAudioActionBuilder withRecordingTerminators(List<Character>  value) {
            this.recordingTerminators = value;
            return this;
        }

        public RecordAudioActionBuilder withBucketName(String value) {
            this.bucketName = value;
            return this;
        }
        
        public RecordAudioActionBuilder withPrefix(String value) {
            this.bucketName = prefix;
            return this;
        }

        @Override
        protected RecordAudioAction buildImpl() {
            return new RecordAudioAction(durationInSeconds, silenceDurationInSeconds, silenceThreshold, recordingTerminators, bucketName, prefix);
        }

    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.RecordAudio;
    }

}
