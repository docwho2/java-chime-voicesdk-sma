/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
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
public class ResponseSpeak implements ResponseAction, ErrorTypeMessage, Serializable {

    private final ResponseActionType type = ResponseActionType.Speak;
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

        @JsonProperty(value = "Text")
        private String text;
        @JsonProperty(value = "CallId")
        private String callId;
        
        @JsonProperty(value = "Engine")
        private Engine engine;
        
        @JsonProperty(value = "LanguageCode")
        private String languageCode;
        
        @JsonProperty(value = "TextType")
        private TextType textType;
        
        @JsonProperty(value = "VoiceId")
        private VoiceId voiceId;
    }

    public enum Engine {
        standard,
        neural
    }

    public enum TextType {
        ssml,
        text
    }

   /**
    * https://docs.aws.amazon.com/polly/latest/dg/API_SynthesizeSpeech.html#polly-SynthesizeSpeech-request-VoiceId
    */
    public enum VoiceId {
        Aditi, Amy, Astrid, Bianca, Brian, Camila, Carla, Carmen, Celine, Chantal,
        Conchita, Cristiano, Dora, Emma, Enrique, Ewa, Filiz, Gabrielle, Geraint,
        Giorgio, Gwyneth, Hans, Ines, Ivy, Jacek, Jan, Joanna, Joey, Justin, Karl,
        Kendra, Kevin, Kimberly, Lea, Liv, Lotte, Lucia, Lupe, Mads, Maja, Marlene,
        Mathieu, Matthew, Maxim, Mia, Miguel, Mizuki, Naja, Nicole, Olivia, Penelope,
        Raveena, Ricardo, Ruben, Russell, Ruth, Salli, Seoyeon, Takumi, Tatyana, Vicki,
        Vitoria, Zeina, Zhiyu, Aria, Ayanda
    }
}
