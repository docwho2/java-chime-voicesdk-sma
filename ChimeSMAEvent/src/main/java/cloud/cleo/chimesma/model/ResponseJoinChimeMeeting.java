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
public class ResponseJoinChimeMeeting implements ResponseAction, ErrorMessage, Serializable {

    private final ResponseActionType type = ResponseActionType.JoinChimeMeeting;

    @JsonProperty(value = "Parameters")
    private Parameters parameters;

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

        @JsonProperty(value = "JoinToken")
        private String joinToken;

        @JsonProperty(value = "CallId")
        private String callId;

        @JsonProperty(value = "ParticipantTag")
        @Builder.Default
        private ParticipantTag participantTag = ParticipantTag.LEG_A;

        @JsonProperty(value = "MeetingId")
        private String meetingId;
    }

}
