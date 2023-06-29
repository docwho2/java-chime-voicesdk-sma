/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.*;
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
public class PlayAudioAction extends Action<PlayAudioAction,ResponsePlayAudio> {

    protected ParticipantTag participantTag;
    protected List<Character> playbackTerminators;
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
        
        final var audioSource = ResponsePlayAudio.AudioSource.builder()
                .withBucketName(bucketName)
                .withKey(myKey)
                .build();

        final var params = ResponsePlayAudio.Parameters.builder()
                .withCallId(getCallId())
                .withAudioSource(audioSource)
                .withParticipantTag(participantTag)
                .withPlaybackTerminators(playbackTerminators)
                .withRepeat(repeat)
                .build();
        return ResponsePlayAudio.builder().withParameters(params).build();
    }
    
    @Override
    protected StringBuilder getDebugSummary() {
        final var sb = super.getDebugSummary();
        
        if ( keyLocale != null ) {
            sb.append(" keyL=[").append(getKeyLocale()).append(']');
        }
        
        if ( key != null ) {
            sb.append(" key=[").append(getKey()).append(']');
        }
        
        if ( bucketName != null ) {
            sb.append(" bucket=[").append(getKey()).append(']');
        }
        
        return sb;       
    }

    public static PlayAudioActionBuilder builder() {
        return new PlayAudioActionBuilder();
    }

    @NoArgsConstructor
    public static class PlayAudioActionBuilder extends ActionBuilder<PlayAudioActionBuilder, PlayAudioAction> {

        protected ParticipantTag participantTag;
        protected List<Character> playbackTerminators;
        protected Integer repeat;

        protected String bucketName = System.getenv("PROMPT_BUCKET");
        protected String key;
        protected String keyLocale;

        public PlayAudioActionBuilder withParticipantTag(ParticipantTag value) {
            this.participantTag = value;
            return this;
        }

        public PlayAudioActionBuilder withPlaybackTerminators(List<Character> value) {
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
