package cloud.cleo.chimesma.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author sjensen
 */
@Data
@Builder(setterPrefix = "with")
@NoArgsConstructor
@AllArgsConstructor
public class ResponseStartCallRecording implements ResponseAction, ErrorMessage, Serializable {

    private final ResponseActionType type = ResponseActionType.StartCallRecording;

    @JsonProperty(value = "Parameters")
    private Parameters parameters;

    // This is used for the incoming ActionData
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonProperty(value = "CallRecordingDestination", access = JsonProperty.Access.WRITE_ONLY)
    private Destination callRecordingDestination;

    // Set on ACTION_FAILED
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonProperty(value = "Error", access = JsonProperty.Access.WRITE_ONLY)
    private String error;

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    public static class Parameters implements Serializable {

        @JsonProperty(value = "CallId")
        private String callId;

        @JsonProperty(value = "Track")
        private Track track;

        @JsonProperty(value = "Destination")
        private Destination destination;
    }

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Destination implements Serializable {

        @JsonProperty(value = "Type")
        private final String type = "S3";

        @JsonProperty(value = "Location")
        @Builder.Default
        private String location = System.getenv("RECORD_BUCKET");

    }

    public enum Track {
        BOTH,
        INCOMING,
        OUTGOING
    }
}
