/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.*;
import cloud.cleo.chimesma.model.ResponseStartCallRecording.Track;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 *
 * @author sjensen
 */
@Data
@SuperBuilder(setterPrefix = "with")
public class StartCallRecordingAction extends Action<StartCallRecordingAction, ResponseStartCallRecording> {

    // Key to store the the recording file in the Transaction Attributes
    public final static String RECORDING_FILE_LOCATION = "RecordingFileLocation";

    @Builder.Default
    protected Track track = Track.BOTH;

    @Builder.Default
    protected String location = System.getenv("RECORD_BUCKET");

    @Builder.Default
    protected Boolean storeLocation = false;

    @Override
    protected ResponseAction getResponse() {
        final var dest = ResponseStartCallRecording.Destination.builder()
                .withLocation(location)
                .build();

        final var params = ResponseStartCallRecording.Parameters.builder()
                .withCallId(getCallId())
                .withTrack(track)
                .withDestination(dest)
                .build();
        return ResponseStartCallRecording.builder().withParameters(params).build();
    }

    @Override
    protected boolean isChainable() {
        // If store location is true, we can't chain, because we need ACTION_SUCCESSFUL event to get the file location
        return !storeLocation;
    }

    @Override
    protected void onActionSuccessful() {
        if (getActionData() != null) {
            final var loc = getActionData().getCallRecordingDestination().getLocation();
            setTransactionAttribute(RECORDING_FILE_LOCATION, loc);
        }
    }

    @Override
    protected StringBuilder getDebugSummary() {
        final var sb = super.getDebugSummary();

        if (track != null) {
            sb.append(" track=[").append(getTrack()).append(']');
        }

        if (storeLocation) {
            sb.append(" storeLoc=[").append(storeLocation).append("]");
        }

        if (location != null) {
            sb.append(" bucket=[").append(getLocation()).append(']');
        }
        return sb;
    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.StartCallRecording;
    }

}
