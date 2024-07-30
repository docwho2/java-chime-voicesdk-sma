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
public class ResponsePause implements ResponseAction, Serializable {
    
    private final ResponseActionType type = ResponseActionType.Pause;
    @JsonProperty(value = "Parameters")
    private Parameters parameters;

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    public static class Parameters implements Serializable {

        @JsonProperty(value = "CallId")
        private String callId;
        @JsonProperty(value = "ParticipantTag")
        @Builder.Default
        private ParticipantTag participantTag = ParticipantTag.LEG_A;
        @JsonProperty(value = "DurationInMilliseconds")
        private Integer durationInMilliseconds;
    }
    
}
