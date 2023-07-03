/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.model;


import cloud.cleo.chimesma.actions.ReceivedDigits;
import cloud.cleo.chimesma.model.ResponsePlayAudio.AudioSource;
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
public class ResponsePlayAudioAndGetDigits implements ResponseAction, ReceivedDigits,ErrorTypeMessage,  Serializable {

    private final ResponseActionType type = ResponseActionType.PlayAudioAndGetDigits;
    
    @JsonProperty(value = "Parameters")
    private Parameters parameters;

     // This is used for the incoming ActionData
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonProperty(value = "ReceivedDigits", access = JsonProperty.Access.WRITE_ONLY)
    private String receivedDigits;
    
    
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

        @JsonProperty(value = "InputDigitsRegex")
        private String inputDigitsRegex;

        @JsonProperty(value = "AudioSource")
        private AudioSource audioSource;

        @JsonProperty(value = "FailureAudioSource")
        private AudioSource failureAudioSource;

        @JsonProperty(value = "MinNumberOfDigits")
        private Integer minNumberOfDigits;

        @JsonProperty(value = "MaxNumberOfDigits")
        private Integer maxNumberOfDigits;

        @JsonProperty(value = "TerminatorDigits")
        private List<Character> terminatorDigits;

        @JsonProperty(value = "InBetweenDigitsDurationInMilliseconds")
        private Integer inBetweenDigitsDurationInMilliseconds;
        
        @JsonProperty(value = "Repeat")
        private Integer repeat;
        
        @JsonProperty(value = "RepeatDurationInMilliseconds")
        private Integer repeatDurationInMilliseconds;

    }

}
