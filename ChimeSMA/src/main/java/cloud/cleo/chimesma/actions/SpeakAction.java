/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import static cloud.cleo.chimesma.actions.AbstractFlow.voice_map;
import cloud.cleo.chimesma.model.ResponseAction;
import cloud.cleo.chimesma.model.ResponseActionType;
import cloud.cleo.chimesma.model.ResponseSpeak;
import cloud.cleo.chimesma.model.ResponseSpeak.Engine;
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
public class SpeakAction extends Action<SpeakAction> {

    protected String text;
    protected Function<SpeakAction, String> textFunction;

    protected Engine engine;
    protected VoiceId voiceId;

    @Override
    public ResponseAction getResponse() {
        var myContent = textFunction != null ? textFunction.apply(this) : text;
        final var params = ResponseSpeak.Parameters.builder()
                .withCallId(callId)
                .withEngine(engine)
                .withLanguageCode(getLocale().toLanguageTag())
                .withText(myContent)
                .withTextType(Action.getSpeakContentType(myContent))
                .withVoiceId(voiceId != null ? voiceId : voice_map.get(getLocale()))
                .build();
        return ResponseSpeak.builder().withParameters(params).build();
    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.Speak;
    }

    @Override
    protected StringBuilder getDebugSummary() {
        final var sb = super.getDebugSummary();

        if (textFunction != null) {
            // Guard against function erroring
            try {
                sb.append(" textF=[").append(textFunction.apply(this)).append(']');
            } catch (Exception e) {
                log.error(this.getClass() + " function error", e);
            }
        } else if (text != null) {
            sb.append(" text=[").append(getText()).append(']');
        }

        if (engine != null) {
            sb.append(" engine=[").append(getEngine()).append(']');
        }

        if (voiceId != null) {
            sb.append(" vid=[").append(getVoiceId()).append(']');
        }

        return sb;
    }

    public static SpeakActionBuilder builder() {
        return new SpeakActionBuilder();
    }

    @NoArgsConstructor
    public static class SpeakActionBuilder extends ActionBuilder<SpeakActionBuilder, SpeakAction> {

        private String text;
        private Function<SpeakAction, String> textFunction;
        private Engine engine;
        private VoiceId voiceId;

        public SpeakActionBuilder withText(String text) {
            this.text = text;
            return this;
        }

        public SpeakActionBuilder withText(Function<SpeakAction, String> text) {
            this.textFunction = text;
            return this;
        }

        public SpeakActionBuilder withEngine(Engine text) {
            this.engine = text;
            return this;
        }

        public SpeakActionBuilder withVoiceId(VoiceId voiceId) {
            this.voiceId = voiceId;
            return this;
        }

        @Override
        protected SpeakAction buildImpl() {
            return new SpeakAction(text, textFunction, engine, voiceId);
        }
    }

}
