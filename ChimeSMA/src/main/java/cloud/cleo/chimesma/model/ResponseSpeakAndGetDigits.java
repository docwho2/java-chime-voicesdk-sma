/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.model;


import cloud.cleo.chimesma.model.ResponseSpeak.Engine;
import cloud.cleo.chimesma.model.ResponseSpeak.TextType;
import cloud.cleo.chimesma.model.ResponseSpeak.VoiceId;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
public class ResponseSpeakAndGetDigits implements ResponseAction, Serializable {

    private final ResponseActionType type = ResponseActionType.SpeakAndGetDigits;

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

        @JsonProperty(value = "InputDigitsRegex")
        private String inputDigitsRegex;

        @JsonProperty(value = "SpeechParameters")
        private SpeechParameter SpeechParameters;

        @JsonProperty(value = "FailureSpeechParameters")
        private SpeechParameter failureSpeechParameters;
        
        
        @JsonProperty(value = "MinNumberOfDigits")
        private Integer minNumberOfDigits;

        @JsonProperty(value = "MaxNumberOfDigits")
        private Integer maxNumberOfDigits;

        @JsonProperty(value = "TerminatorDigits")
        private List<String> terminatorDigits;

        @JsonProperty(value = "InBetweenDigitsDurationInMilliseconds")
        private Integer inBetweenDigitsDurationInMilliseconds;
        
        @JsonProperty(value = "Repeat")
        private Integer repeat;
        
        @JsonProperty(value = "RepeatDurationInMilliseconds")
        private Integer repeatDurationInMilliseconds;
        

        @Data
        @Builder(setterPrefix = "with")
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(value = JsonInclude.Include.NON_NULL)
        public static class SpeechParameter implements Serializable {

            @JsonProperty(value = "Text")
            private String text;

            @JsonProperty(value = "Engine")
            private Engine engine;
            
            @JsonProperty(value = "LanguageCode")
            private String languageCode;
            
            @JsonProperty(value = "TextType")
            private TextType textType;
            
            @JsonProperty(value = "VoiceId")
            private VoiceId voiceId;
            
        }
    }

}
