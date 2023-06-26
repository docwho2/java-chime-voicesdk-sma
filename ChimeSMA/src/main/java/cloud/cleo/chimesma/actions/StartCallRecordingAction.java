/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.*;
import cloud.cleo.chimesma.model.ResponseStartCallRecording.Track;
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
public class StartCallRecordingAction extends Action<StartCallRecordingAction> {

    // Key to store the the recording file in the Transaction Attributes
    public final static String RECORDING_FILE_LOCATION = "RecordingFileLocation";
    
    protected Track track;
    protected String location = System.getenv("RECORD_BUCKET");
    protected Boolean storeLocation = false;

    @Override
    public ResponseAction getResponse() {
        final var dest = ResponseStartCallRecording.Parameters.Destination.builder()
                .withLocation(location)
                .build();

        final var params = ResponseStartCallRecording.Parameters.builder()
                .withCallId(callId)
                .withTrack(track)
                .withDestination(dest)
                .build();
        return ResponseStartCallRecording.builder().withParameters(params).build();
    }
    
    @Override
    protected boolean isChainable() {
        // If store location is true, we can't chain, because we need ACTION_SUCCESSFUL event to get the file location
        return ! storeLocation;
    }
    
    @Override
    protected StringBuilder getDebugSummary() {
        final var sb = super.getDebugSummary();
        
        if ( track != null ) {
            sb.append(" track=[").append(getTrack()).append(']');
        }
        
        if ( storeLocation ) {
            sb.append(" storeLoc=[true]");
        }
        
        if (location != null ) {
            sb.append(" bucket=[").append(getLocation()).append(']');
        }
        return sb;       
    }

    public static StartCallRecordingActionBuilder builder() {
        return new StartCallRecordingActionBuilder();
    }

    @NoArgsConstructor
    public static class StartCallRecordingActionBuilder extends ActionBuilder<StartCallRecordingActionBuilder, StartCallRecordingAction> {

        private Track track;
        private String location = System.getenv("RECORD_BUCKET");
        private Boolean storeLocation = false;

        public StartCallRecordingActionBuilder withTrack(Track value) {
            this.track = value;
            return this;
        }

        public StartCallRecordingActionBuilder withLocation(String value) {
            this.location = value;
            return this;
        }
        
        public StartCallRecordingActionBuilder withStoreLocation() {
            this.storeLocation = true;
            return this;
        }

        @Override
        protected StartCallRecordingAction buildImpl() {
            return new StartCallRecordingAction(track, location,storeLocation);
        }

    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.StartCallRecording;
    }

}
