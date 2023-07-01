/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.*;
import java.util.List;
import java.util.function.Function;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 *
 * @author sjensen
 */
@Data
@SuperBuilder(setterPrefix = "with")
public class PlayAudioAction extends Action<PlayAudioAction, ResponsePlayAudio> {

    protected List<Character> playbackTerminators;
    protected Integer repeat;

    @Builder.Default
    protected String bucketName = System.getenv("PROMPT_BUCKET");
    protected Function<PlayAudioAction, String> bucketNameF;
    
    protected String key;
    protected Function<PlayAudioAction, String> keyF;
    
    protected String keyLocale;
    protected Function<PlayAudioAction, String> keyLocaleF;

    @Override
    protected ResponseAction getResponse() {
        final String myKey;
        if ( getFuncValOrDefault(keyLocaleF, keyLocale) != null) {
            myKey = getFuncValOrDefault(keyLocaleF, keyLocale) + "-" + getLocale().toLanguageTag() + ".wav";
        } else {
            myKey = getFuncValOrDefault(keyF, key);
        }

        final var audioSource = ResponsePlayAudio.AudioSource.builder()
                .withBucketName(getFuncValOrDefault(bucketNameF, bucketName))
                .withKey(myKey)
                .build();

        final var params = ResponsePlayAudio.Parameters.builder()
                .withCallId(getCallId())
                .withAudioSource(audioSource)
                .withPlaybackTerminators(playbackTerminators)
                .withRepeat(repeat)
                .build();
        return ResponsePlayAudio.builder().withParameters(params).build();
    }

    @Override
    protected StringBuilder getDebugSummary() {
        final var sb = super.getDebugSummary();

        if ( getFuncValOrDefault(keyLocaleF, keyLocale) != null) {
            sb.append(" keyL=[").append(getFuncValOrDefault(keyLocaleF, keyLocale)).append(']');
        }

        if (getFuncValOrDefault(keyF, key) != null) {
            sb.append(" key=[").append(getFuncValOrDefault(keyF, key) ).append(']');
        }

        if (getFuncValOrDefault(bucketNameF, bucketName) != null) {
            sb.append(" bucket=[").append(getFuncValOrDefault(bucketNameF, bucketName)).append(']');
        }

        return sb;
    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.PlayAudio;
    }

}
