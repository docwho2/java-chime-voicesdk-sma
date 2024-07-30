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
import lombok.experimental.SuperBuilder;

/**
 *
 * @author sjensen
 */
@Data
@SuperBuilder(setterPrefix = "with")
public class SpeakAndGetDigitsAction extends Action<SpeakAndGetDigitsAction, ResponseSpeakAndGetDigits> implements ReceivedDigits {

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
    protected ResponseAction getResponse() {

        final List<ResponseSpeakAndGetDigits.SpeechParameter> resp = new LinkedList<>();
        for (final SpeechParameters sp : List.of(speechParameters, failureSpeechParameters)) {
            final var myContent = getFuncValOrDefault(sp.textF, sp.text);
            final var locale = sp.locale != null ? sp.locale : getLocale();
            final var speechParam = ResponseSpeakAndGetDigits.SpeechParameter.builder()
                    .withText(myContent)
                    .withTextType(getSpeakContentType(myContent))
                    .withEngine(sp.engine != null ? sp.engine : Engine.neural)
                    // If set on the builder, use that, otherwise our the Actions locale
                    .withLanguageCode(locale.toLanguageTag())
                    .withVoiceId(sp.voiceId != null ? sp.voiceId : voice_map.get(locale))
                    .build();

            resp.add(speechParam);
        }

        final var it = resp.iterator();
        final var params = ResponseSpeakAndGetDigits.Parameters.builder()
                .withCallId(getCallId())
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

        sb.append(" text=[").append(getFuncValOrDefault(speechParameters.textF, speechParameters.text)).append(']');

        if (inputDigitsRegex != null) {
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

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(setterPrefix = "with")
    public static class SpeechParameters {

        private String text;
        private Function<SpeakAndGetDigitsAction, String> textF;
        private Engine engine;
        private Locale locale;
        private VoiceId voiceId;
    }
    
    /**
     * Given message content, determine if the message is SSML or just plain text
     *
     * @param message
     * @return
     */
    private static ResponseSpeak.TextType getSpeakContentType(String message) {
        if (message != null) {
            return message.toLowerCase().contains("<speak>") ? ResponseSpeak.TextType.ssml : ResponseSpeak.TextType.text;
        }
        return ResponseSpeak.TextType.text;
    }

}
