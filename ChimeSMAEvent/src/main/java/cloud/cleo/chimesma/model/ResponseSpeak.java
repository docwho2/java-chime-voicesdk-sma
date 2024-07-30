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
     *
     * These can change over time, run below to get nice formatted and sorted list to paste .
     *
     * aws polly describe-voices --query 'Voices[].Id' --output json | tr -d '\"[] ' | sort | xargs -n 7
     */
    public enum VoiceId {
        Aditi, Adriano, Amy, Andres, Aria, Arlet, Arthur,
        Astrid, Ayanda, Bianca, Brian, Burcu, Camila, Carla,
        Carmen, Celine, Chantal, Conchita, Cristiano, Daniel, Danielle,
        Dora, Elin, Emma, Enrique, Ewa, Filiz, Gabrielle,
        Geraint, Giorgio, Gregory, Gwyneth, Hala, Hannah, Hans,
        Hiujin, Ida, Ines, Isabelle, Ivy, Jacek, Jan,
        Joanna, Joey, Justin, Kajal, Karl, Kazuha, Kendra,
        Kevin, Kimberly, Laura, Lea, Liam, Lisa, Liv,
        Lotte, Lucia, Lupe, Mads, Maja, Marlene, Mathieu,
        Matthew, Maxim, Mia, Miguel, Mizuki, Naja, Niamh,
        Nicole, Ola, Olivia, Pedro, Penelope, Raveena, Remi,
        Ricardo, Ruben, Russell, Ruth, Salli, Seoyeon, Sergio,
        Sofie, Stephen, Suvi, Takumi, Tatyana, Thiago, Tomoko,
        Vicki, Vitoria, Zayd, Zeina, Zhiyu
    }
}
