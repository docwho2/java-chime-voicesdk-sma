/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma;

import cloud.cleo.chimesma.model.ResponseAction;
import cloud.cleo.chimesma.model.ResponseActionType;
import cloud.cleo.chimesma.model.ResponseSpeak;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.polly.model.Engine;
import software.amazon.awssdk.services.polly.model.LanguageCode;
import software.amazon.awssdk.services.polly.model.TextType;
import software.amazon.awssdk.services.polly.model.VoiceId;

/**
 *
 * @author sjensen
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpeakAction extends Action {

    @JsonProperty(value = "Text")
    private String text;
    private Function<SpeakAction,String> textFunction;
    
    @JsonProperty(value = "Engine")
    private Engine engine;
    @JsonProperty(value = "LanguageCode")
    private LanguageCode languageCode;
    @JsonProperty(value = "TextType")
    private TextType textType;
    @JsonProperty(value = "VoiceId")
    private VoiceId voiceId;

    @Override
    public ResponseAction getResponse() {
        final var params = ResponseSpeak.Parameters.builder()
                .withCallId(callId)
                .withEngine(engine)
                .withLanguageCode(languageCode)
                .withText(textFunction != null ? textFunction.apply(this) : text)
                .withTextType(textType)
                .withVoiceId(voiceId)
                .build();
        return ResponseSpeak.builder().withParameters(params).build();
    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.Speak;
    }
    
    @Override
    protected StringBuilder getDebugSummary() {
        return super.getDebugSummary()
                .append(" [").append(getText()).append(']');
    }

    public static SpeakActionBuilder builder() {
        return new SpeakActionBuilder();
    }

    @NoArgsConstructor
    public static class SpeakActionBuilder extends ActionBuilder<SpeakActionBuilder,SpeakAction> {

        private String text;
        private Function<SpeakAction,String> textFunction;
        private Engine engine;
        private LanguageCode languageCode;
        private TextType textType;
        private VoiceId voiceId;
        
        public SpeakActionBuilder withText(String text) {
            this.text = text;
            return this;
        }
        
        public SpeakActionBuilder withText(Function<SpeakAction,String> text) {
            this.textFunction = text;
            return this;
        }
        
        public SpeakActionBuilder withVoiceId(VoiceId voiceId) {
            this.voiceId = voiceId;
            return this;
        }
        
        @Override
        protected SpeakAction buildImpl() {
            return new SpeakAction(text, textFunction, engine, languageCode, textType, voiceId);
        }
    }

}
