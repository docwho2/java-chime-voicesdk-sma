package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.*;
import cloud.cleo.chimesma.model.ResponsePlayAudio.AudioSource;
import java.util.LinkedList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 *
 * @author sjensen
 */
@Data
@SuperBuilder(setterPrefix = "with")
public class PlayAudioAndGetDigitsAction extends Action<PlayAudioAndGetDigitsAction, ResponsePlayAudioAndGetDigits> implements ReceivedDigits {

    protected ParticipantTag participantTag;

    protected String inputDigitsRegex;
    protected AudioSourceLocale audioSource;
    protected AudioSourceLocale failureAudioSource;

    protected Integer minNumberOfDigits;
    protected Integer maxNumberOfDigits;
    protected List<Character> terminatorDigits;
    protected Integer inBetweenDigitsDurationInMilliseconds;
    protected Integer repeat;
    protected Integer repeatDurationInMilliseconds;

    @Override
    protected ResponseAction getResponse() {

        final List<AudioSource> resp = new LinkedList<>();
        for (final AudioSourceLocale as : List.of(audioSource, failureAudioSource)) {
            final String myKey;
            if (as.keyLocale != null) {
                myKey = as.keyLocale + "-" + getLocale().toLanguageTag() + ".wav";
            } else {
                myKey = as.getKey();
            }
            final var audio = AudioSource.builder()
                    .withBucketName(as.getBucketName())
                    .withKey(myKey)
                    .build();

            resp.add(audio);
        }

        final var it = resp.iterator();
        final var params = ResponsePlayAudioAndGetDigits.Parameters.builder()
                .withCallId(getCallId())
                .withInputDigitsRegex(inputDigitsRegex)
                .withAudioSource(it.next())
                .withFailureAudioSource(it.next())
                .withMinNumberOfDigits(minNumberOfDigits)
                .withMaxNumberOfDigits(maxNumberOfDigits)
                .withTerminatorDigits(terminatorDigits)
                .withInBetweenDigitsDurationInMilliseconds(inBetweenDigitsDurationInMilliseconds)
                .withRepeat(repeat)
                .withRepeatDurationInMilliseconds(repeatDurationInMilliseconds)
                .build();
        return ResponsePlayAudioAndGetDigits.builder().withParameters(params).build();
    }

    @Override
    public String getReceivedDigits() {
        return getRecievedDigitsFromAction();
    }

    @Override
    protected boolean isChainable() {
        return false;
    }

    @Override
    protected StringBuilder getDebugSummary() {
        return super.getDebugSummary()
                .append(" [").append(getInputDigitsRegex()).append(']');
    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.PlayAudioAndGetDigits;
    }

    @Data
    @Builder(setterPrefix = "with")
    public static class AudioSourceLocale {

        @Builder.Default
        private String bucketName = System.getenv("PROMPT_BUCKET");
        private String key;
        private String keyLocale;
    }
}
