/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.*;
import cloud.cleo.chimesma.model.ResponsePlayAudio.AudioSource;
import java.util.LinkedList;
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
@NoArgsConstructor
@AllArgsConstructor
public class PlayAudioAndGetDigitsAction extends Action<PlayAudioAndGetDigitsAction> implements ReceivedDigits {

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
    public ResponseAction getResponse() {

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
                .withCallId(callId)
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

    public static PlayAudioAndGetDigitsActionBuilder builder() {
        return new PlayAudioAndGetDigitsActionBuilder();
    }

    @NoArgsConstructor
    public static class PlayAudioAndGetDigitsActionBuilder extends ActionBuilder<PlayAudioAndGetDigitsActionBuilder, PlayAudioAndGetDigitsAction> {

        private ParticipantTag participantTag;

        private String inputDigitsRegex;
        AudioSourceLocale audioSource;
        AudioSourceLocale failureAudioSource;

        private Integer minNumberOfDigits;
        private Integer maxNumberOfDigits;
        private List<Character> terminatorDigits;
        private Integer inBetweenDigitsDurationInMilliseconds;
        private Integer repeat;
        private Integer repeatDurationInMilliseconds;

        public PlayAudioAndGetDigitsActionBuilder withParticipantTag(ParticipantTag value) {
            this.participantTag = value;
            return this;
        }

        public PlayAudioAndGetDigitsActionBuilder withInputDigitsRegex(String text) {
            this.inputDigitsRegex = text;
            return this;
        }

        public PlayAudioAndGetDigitsActionBuilder withAudioSource(AudioSourceLocale value) {
            this.audioSource = value;
            return this;
        }

        public PlayAudioAndGetDigitsActionBuilder withFailureAudioSource(AudioSourceLocale value) {
            this.failureAudioSource = value;
            return this;
        }

        public PlayAudioAndGetDigitsActionBuilder withMinNumberOfDigits(Integer value) {
            this.minNumberOfDigits = value;
            return this;
        }

        public PlayAudioAndGetDigitsActionBuilder withMaxNumberOfDigits(Integer value) {
            this.maxNumberOfDigits = value;
            return this;
        }

        public PlayAudioAndGetDigitsActionBuilder withTerminatorDigits(List<Character> value) {
            this.terminatorDigits = value;
            return this;
        }

        public PlayAudioAndGetDigitsActionBuilder withInBetweenDigitsDurationInMilliseconds(Integer value) {
            this.inBetweenDigitsDurationInMilliseconds = value;
            return this;
        }

        public PlayAudioAndGetDigitsActionBuilder withRepeat(Integer value) {
            this.repeat = value;
            return this;
        }

        public PlayAudioAndGetDigitsActionBuilder withRepeatDurationInMilliseconds(Integer value) {
            this.repeatDurationInMilliseconds = value;
            return this;
        }

        @Override
        protected PlayAudioAndGetDigitsAction buildImpl() {
            return new PlayAudioAndGetDigitsAction(participantTag, inputDigitsRegex, audioSource, failureAudioSource, minNumberOfDigits, maxNumberOfDigits, terminatorDigits, inBetweenDigitsDurationInMilliseconds, repeat, repeatDurationInMilliseconds);
        }
    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.PlayAudioAndGetDigits;
    }

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioSourceLocale  {
        @Builder.Default
        private String bucketName = System.getenv("PROMPT_BUCKET");
        private String key;
        private String keyLocale;
    }
}
