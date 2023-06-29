/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.*;
import java.util.List;
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
public class PlayAudioAction extends Action<PlayAudioAction,ResponsePlayAudio> {

    protected ParticipantTag participantTag;
    protected List<Character> playbackTerminators;
    protected Integer repeat;

     @Builder.Default
    protected String bucketName = System.getenv("PROMPT_BUCKET");
    protected String key;
    protected String keyLocale;

    @Override
    protected ResponseAction getResponse() {
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

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.PlayAudio;
    }

}
