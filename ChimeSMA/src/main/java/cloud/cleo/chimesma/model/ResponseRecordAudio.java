/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author sjensen
 */
@Data
@Builder(setterPrefix = "with")
@NoArgsConstructor
@AllArgsConstructor
public class ResponseRecordAudio implements ResponseAction, Serializable {

    private final ResponseActionType type = ResponseActionType.RecordAudio;

    @JsonProperty(value = "Parameters")
    private Parameters parameters;

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    public static class Parameters implements Serializable {

        @JsonProperty(value = "CallId")
        private String callId;

        @JsonProperty(value = "DurationInSeconds")
        private Integer durationInSeconds;

        @JsonProperty(value = "SilenceDurationInSeconds")
        private Integer silenceDurationInSeconds;

        @JsonProperty(value = "SilenceThreshold")
        private Integer silenceThreshold;

        @JsonProperty(value = "RecordingTerminators")
        private List<Character> recordingTerminators;

        @JsonProperty(value = "RecordingDestination")
        private RecordingDestination recordingDestination;

        @Data
        @Builder(setterPrefix = "with")
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RecordingDestination implements Serializable {

            @JsonProperty(value = "Type")
            private final String type = "S3";

            @JsonProperty(value = "BucketName")
            @Builder.Default
            private String bucketName = System.getenv("RECORD_BUCKET");

            @JsonProperty(value = "Prefix")
            private String prefix;
        }
    }

}
