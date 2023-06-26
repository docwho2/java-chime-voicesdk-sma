/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import static cloud.cleo.chimesma.actions.AbstractFlow.voice_map;
import cloud.cleo.chimesma.model.*;
import cloud.cleo.chimesma.model.ResponseSpeak.Engine;
import cloud.cleo.chimesma.model.ResponseSpeak.VoiceId;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author sjensen
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpeakAndGetDigitsAction extends Action<SpeakAndGetDigitsAction> implements ReceivedDigits {

    protected String inputDigitsRegex;

    protected SpeechParameters speechParameters;
    protected SpeechParameters failureSpeechParameters;

    protected Integer minNumberOfDigits;
    protected Integer maxNumberOfDigits;
    protected List<Character> terminatorDigits;
    protected Integer inBetweenDigitsDurationInMilliseconds;
    protected Integer repeat;
    protected Integer repeatDurationInMilliseconds;

    @Override
    public ResponseAction getResponse() {

        final List<ResponseSpeakAndGetDigits.Parameters.SpeechParameter> resp = new LinkedList<>();
        for (final SpeechParameters sp : List.of(speechParameters, failureSpeechParameters)) {
            final var myContent = sp.textFunction != null ? sp.textFunction.apply(this) : sp.text;
            final var locale = sp.locale != null ? sp.locale : getLocale();
            final var speechParam = ResponseSpeakAndGetDigits.Parameters.SpeechParameter.builder()
                    .withText(myContent)
                    .withTextType(Action.getSpeakContentType(myContent))
                    .withEngine(sp.engine)
                    // If set on the builder, use that, otherwise our the Actions locale
                    .withLanguageCode(locale.toLanguageTag())
                    .withVoiceId(sp.voiceId != null ? sp.voiceId : voice_map.get(locale))
                    .build();
         
             resp.add(speechParam);
        }

        final var it = resp.iterator();
        final var params = ResponseSpeakAndGetDigits.Parameters.builder()
                .withCallId(callId)
                .withInputDigitsRegex(inputDigitsRegex)
                .withSpeechParameters(it.next())
                .withFailureSpeechParameters(it.next())
                .withMinNumberOfDigits(minNumberOfDigits)
                .withMaxNumberOfDigits(maxNumberOfDigits)
                .withTerminatorDigits(terminatorDigits)
                .withInBetweenDigitsDurationInMilliseconds(inBetweenDigitsDurationInMilliseconds)
                .withRepeat(repeat)
                .withRepeatDurationInMilliseconds(repeatDurationInMilliseconds)
                .build();
        return ResponseSpeakAndGetDigits.builder().withParameters(params).build();
    }

    @Override
    public String getReceivedDigits() {
       return getRecievedDigitsFromAction();
    }
    
    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.SpeakAndGetDigits;
    }

    @Override
    protected StringBuilder getDebugSummary() {
        final var sb = super.getDebugSummary();

        if (speechParameters.textFunction != null) {
            // Guard against function erroring
            try {
                sb.append(" textF=[").append(speechParameters.textFunction.apply(this)).append(']');
            } catch (Exception e) {
                log.error(this.getClass() + " function error", e);
            }
        } else if (speechParameters.text != null) {
            sb.append(" text=[").append(speechParameters.text).append(']');
        }
        
        if ( inputDigitsRegex != null ) {
            sb.append(" re=[").append(getInputDigitsRegex()).append(']');
        }

        if (speechParameters.engine != null) {
            sb.append(" engine=[").append(speechParameters.engine).append(']');
        }

        if (speechParameters.voiceId != null) {
            sb.append(" vid=[").append(speechParameters.voiceId).append(']');
        }

        return sb;
    }

    @Override
    protected boolean isChainable() {
        return false;
    }

    public static SpeakAndGetDigitsActionBuilder builder() {
        return new SpeakAndGetDigitsActionBuilder();
    }

    @NoArgsConstructor
    public static class SpeakAndGetDigitsActionBuilder extends ActionBuilder<SpeakAndGetDigitsActionBuilder, SpeakAndGetDigitsAction> {

        private String inputDigitsRegex;
        SpeechParameters speechParameters;
        SpeechParameters failureSpeechParameters;
        private Integer minNumberOfDigits;
        private Integer maxNumberOfDigits;
        private List<Character> terminatorDigits;
        private Integer inBetweenDigitsDurationInMilliseconds;
        private Integer repeat;
        private Integer repeatDurationInMilliseconds;

        public SpeakAndGetDigitsActionBuilder withInputDigitsRegex(String text) {
            this.inputDigitsRegex = text;
            return this;
        }

        public SpeakAndGetDigitsActionBuilder withSpeechParameters(SpeechParameters value) {
            this.speechParameters = value;
            return this;
        }

        public SpeakAndGetDigitsActionBuilder withFailureSpeechParameters(SpeechParameters value) {
            this.failureSpeechParameters = value;
            return this;
        }

        public SpeakAndGetDigitsActionBuilder withMinNumberOfDigits(Integer value) {
            this.minNumberOfDigits = value;
            return this;
        }

        public SpeakAndGetDigitsActionBuilder withMaxNumberOfDigits(Integer value) {
            this.maxNumberOfDigits = value;
            return this;
        }

        public SpeakAndGetDigitsActionBuilder withTerminatorDigits(List<Character> value) {
            this.terminatorDigits = value;
            return this;
        }

        public SpeakAndGetDigitsActionBuilder withInBetweenDigitsDurationInMilliseconds(Integer value) {
            this.inBetweenDigitsDurationInMilliseconds = value;
            return this;
        }

        public SpeakAndGetDigitsActionBuilder withRepeat(Integer value) {
            this.repeat = value;
            return this;
        }

        public SpeakAndGetDigitsActionBuilder withRepeatDurationInMilliseconds(Integer value) {
            this.repeatDurationInMilliseconds = value;
            return this;
        }

        @Override
        protected SpeakAndGetDigitsAction buildImpl() {
            return new SpeakAndGetDigitsAction(inputDigitsRegex, speechParameters, failureSpeechParameters, minNumberOfDigits, maxNumberOfDigits, terminatorDigits, inBetweenDigitsDurationInMilliseconds, repeat, repeatDurationInMilliseconds);
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(setterPrefix = "with")
    public static class SpeechParameters {

        private String text;
        private Function<SpeakAndGetDigitsAction, String> textFunction;
        private Engine engine;
        private Locale locale;
        private VoiceId voiceId;
    }

}
