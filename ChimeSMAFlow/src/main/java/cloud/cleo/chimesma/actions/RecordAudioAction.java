package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.*;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * Record Audio Action
 *
 *
 * @author sjensen
 * @see <a href="https://docs.aws.amazon.com/chime-sdk/latest/dg/record-audio.html">AWS Documentation</a>
 */
@Data()
@SuperBuilder(setterPrefix = "with")
public class RecordAudioAction extends Action<RecordAudioAction, ResponseRecordAudio> {

    // Keys to store the the recording file in the Transaction Attributes
    public final static String RECORD_AUDIO_BUCKET = "RecordAudioBucket";
    public final static String RECORD_AUDIO_KEY = "RecordAudioKey";
    public final static String RECORD_AUDIO_TERMINATOR = "RecordAudioTerm";

    /**
     * Description – The duration of the recording, in seconds.
     * <p>
     * Allowed values – >0
     * <p>
     * Required – No
     * <p>
     * Default value – None
     */
    private Integer durationInSeconds;
    private Integer silenceDurationInSeconds;
    private Integer silenceThreshold;
    private List<Character> recordingTerminators;
    @Builder.Default
    private String bucketName = System.getenv("RECORD_BUCKET");
    private String prefix;

    @Override
    protected ResponseAction getResponse() {

        final var dest = ResponseRecordAudio.RecordingDestination.builder()
                .withBucketName(bucketName)
                .withPrefix(prefix)
                .build();

        final var params = ResponseRecordAudio.Parameters.builder()
                .withCallId(getCallId())
                .withDurationInSeconds(durationInSeconds)
                .withSilenceDurationInSeconds(silenceDurationInSeconds)
                .withSilenceThreshold(silenceThreshold)
                .withRecordingTerminators(recordingTerminators)
                .withRecordingDestination(dest)
                .build();
        return ResponseRecordAudio.builder().withParameters(params).build();
    }

    @Override
    protected boolean isChainable() {
        return false;
    }

    @Override
    protected void onActionSuccessful() {
        final var ad = getActionData();
        if (ad != null) {
            final var rd = ad.getRecordingDestination();
            log.debug("Record Audio SUCCESS with file " + rd.getKey());
            setTransactionAttribute(RECORD_AUDIO_BUCKET, rd.getBucketName());
            setTransactionAttribute(RECORD_AUDIO_KEY, rd.getKey());
            // Will be null silence detection or max length occurs (IE, nothing was press, but we have a file, all is good
            if( ad.getRecordingTerminatorUsed() != null ) {
                setTransactionAttribute(RECORD_AUDIO_TERMINATOR, ad.getRecordingTerminatorUsed().toString());
            }
        }
    }

    @Override
    protected StringBuilder getDebugSummary() {
        final var sb = super.getDebugSummary();

        if (prefix != null) {
            sb.append(" prefix=[").append(getPrefix()).append(']');
        }

        return sb;
    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.RecordAudio;
    }

}
