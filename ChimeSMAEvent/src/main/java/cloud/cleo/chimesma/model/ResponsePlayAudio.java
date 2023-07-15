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
public class ResponsePlayAudio implements ResponseAction, ErrorTypeMessage, Serializable {

    private final ResponseActionType type = ResponseActionType.PlayAudio;

    @JsonProperty(value = "Parameters")
    private Parameters parameters;

    // Set on ACTION_FAILED
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonProperty(value = "ErrorType", access = JsonProperty.Access.WRITE_ONLY)
    private String errorType;
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonProperty(value = "ErrorMessage", access = JsonProperty.Access.WRITE_ONLY)
    private String errorMessage;

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    public static class Parameters implements Serializable {

        @JsonProperty(value = "CallId")
        private String callId;
        @JsonProperty(value = "ParticipantTag")
        @Builder.Default
        private ParticipantTag participantTag = ParticipantTag.LEG_A;
        @JsonProperty(value = "PlaybackTerminators")
        private List<Character> playbackTerminators;
        @JsonProperty(value = "Repeat")
        private Integer repeat;
        @JsonProperty(value = "AudioSource")
        private AudioSource audioSource;

    }

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioSource implements Serializable {

        @JsonProperty(value = "Type")
        private final String type = "S3";

        @JsonProperty(value = "BucketName")
        @Builder.Default
        private String bucketName = System.getenv("PROMPT_BUCKET");
        @JsonProperty(value = "Key")
        private String key;
    }

}
