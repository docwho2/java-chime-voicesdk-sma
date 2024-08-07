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
public class ResponseReceiveDigits implements ResponseAction, ReceivedDigits, Serializable {
    
    private final ResponseActionType type = ResponseActionType.ReceiveDigits;
    @JsonProperty(value = "Parameters")
    private Parameters parameters;

     // This is used for the incoming ActionData
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonProperty(value = RECEIVED_DIGITS, access = JsonProperty.Access.WRITE_ONLY)
    private String receivedDigits;
    
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
        @JsonProperty(value = "InputDigitsRegex")
        private String inputDigitsRegex;
        @JsonProperty(value = "InBetweenDigitsDurationInMilliseconds")
        private Integer inBetweenDigitsDurationInMilliseconds;
        @JsonProperty(value = "FlushDigitsDurationInMilliseconds")
        private Integer flushDigitsDurationInMilliseconds;
    }
    
}
