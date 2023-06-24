/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.ResponseAction;
import cloud.cleo.chimesma.model.ResponseActionType;
import cloud.cleo.chimesma.model.ResponseSpeak;
import cloud.cleo.chimesma.model.ResponseSpeak.Engine;
import cloud.cleo.chimesma.model.ResponseSpeak.TextType;
import cloud.cleo.chimesma.model.ResponseSpeak.VoiceId;
import java.util.function.Function;
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
public class SpeakAction extends Action {

    private String text;
    private Function<SpeakAction,String> textFunction;

    private Engine engine;
    private String languageCode;
    

    private TextType textType;
    private VoiceId voiceId;

    @Override
    public ResponseAction getResponse() {
        var myContent = textFunction != null ? textFunction.apply(this) : text;
        final var params = ResponseSpeak.Parameters.builder()
                .withCallId(callId)
                .withEngine(engine)
                .withLanguageCode(languageCode)
                .withText(myContent)
                .withTextType(Action.getSpeakContentType(myContent))
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
        private String languageCode;
        private VoiceId voiceId;
        
        public SpeakActionBuilder withText(String text) {
            this.text = text;
            return this;
        }
        
        public SpeakActionBuilder withText(Function<SpeakAction,String> text) {
            this.textFunction = text;
            return this;
        }
        
        public SpeakActionBuilder withEngine(Engine text) {
            this.engine = text;
            return this;
        }
        
        public SpeakActionBuilder withLanguageCode(String text) {
            this.languageCode = text;
            return this;
        }
        
        
        public SpeakActionBuilder withVoiceId(VoiceId voiceId) {
            this.voiceId = voiceId;
            return this;
        }
        
        @Override
        protected SpeakAction buildImpl() {
            return new SpeakAction(text, textFunction, engine, languageCode, null, voiceId);
        }
    }

}
