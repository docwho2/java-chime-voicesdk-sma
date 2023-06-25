/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.ResponseAction;
import cloud.cleo.chimesma.model.ResponseActionType;
import cloud.cleo.chimesma.model.ResponsePlayAudio;
import java.util.List;
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
public class PlayAudioAction extends Action<PlayAudioAction> {

    protected ParticipantTag participantTag;
    protected List<String> playbackTerminators;
    protected Integer repeat;

    protected String bucketName = System.getenv("PROMPT_BUCKET");
    protected String key;
    protected String keyLocale;

    @Override
    public ResponseAction getResponse() {
        final String myKey;
        if ( keyLocale != null ) {
            myKey = keyLocale + "-" + getLocale().toLanguageTag() + ".wav";
        } else {
            myKey = key;
        }
        
        final var audioSource = ResponsePlayAudio.Parameters.AudioSource.builder()
                .withBucketName(bucketName)
                .withKey(myKey)
                .build();

        final var params = ResponsePlayAudio.Parameters.builder()
                .withCallId(callId)
                .withAudioSource(audioSource)
                .withParticipantTag(participantTag)
                .withPlaybackTerminators(playbackTerminators)
                .withRepeat(repeat)
                .build();
        return ResponsePlayAudio.builder().withParameters(params).build();
    }
    
    @Override
    protected StringBuilder getDebugSummary() {
        return super.getDebugSummary()
                .append(" [").append(getKey()).append(']');
    }

    public static PlayAudioActionBuilder builder() {
        return new PlayAudioActionBuilder();
    }

    @NoArgsConstructor
    public static class PlayAudioActionBuilder extends ActionBuilder<PlayAudioActionBuilder, PlayAudioAction> {

        protected ParticipantTag participantTag;
        protected List<String> playbackTerminators;
        protected Integer repeat;

        protected String bucketName = System.getenv("PROMPT_BUCKET");
        protected String key;
        protected String keyLocale;

        public PlayAudioActionBuilder withParticipantTag(ParticipantTag value) {
            this.participantTag = value;
            return this;
        }

        public PlayAudioActionBuilder withPlaybackTerminators(List<String> value) {
            this.playbackTerminators = value;
            return this;
        }

        public PlayAudioActionBuilder withRepeat(Integer value) {
            this.repeat = value;
            return this;
        }

        public PlayAudioActionBuilder withBucketName(String value) {
            this.bucketName = value;
            return this;
        }

        public PlayAudioActionBuilder withKey(String value) {
            this.key = value;
            return this;
        }
        
        public PlayAudioActionBuilder withKeyLocale(String value) {
            this.keyLocale = value;
            return this;
        }

        @Override
        protected PlayAudioAction buildImpl() {
            return new PlayAudioAction(participantTag, playbackTerminators, repeat, bucketName, key, keyLocale);
        }
    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.PlayAudio;
    }

}
