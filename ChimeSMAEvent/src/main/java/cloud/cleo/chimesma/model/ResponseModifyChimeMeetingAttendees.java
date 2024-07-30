package cloud.cleo.chimesma.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
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
@Builder(setterPrefix = "with")
@NoArgsConstructor
@AllArgsConstructor
public class ResponseModifyChimeMeetingAttendees implements ResponseAction, ErrorTypeMessage, Serializable {

    private final ResponseActionType type = ResponseActionType.ModifyChimeMeetingAttendees;

    @JsonProperty(value = "Parameters")
    private Parameters parameters;

    
    // Set on ACTION_FAILED
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonProperty(value = "ErrorType", access = JsonProperty.Access.WRITE_ONLY)
    private String errorType;
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonProperty(value = "ErrorMessage", access = JsonProperty.Access.WRITE_ONLY)
    private String errorMessage;
    
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonProperty(value = "ErrorList", access = JsonProperty.Access.WRITE_ONLY)
    private List<String> errorList;
    
    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    public static class Parameters implements Serializable {

        @JsonProperty(value = "Operation")
        private Operation operation;

        @JsonProperty(value = "MeetingId")
        private String meetingId;

        @JsonProperty(value = "CallId")
        private String callId;

        @JsonProperty(value = "ParticipantTag")
        @Builder.Default
        private ParticipantTag participantTag = ParticipantTag.LEG_A;

        @JsonProperty(value = "AttendeeList")
        private List<String> attendeeList;
        
    }

    public static enum Operation {
        Mute,
        Unmute
    }
}
