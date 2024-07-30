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
public class ResponseSendDigits implements ResponseAction, ErrorTypeMessage, Serializable {
    
    private final ResponseActionType type = ResponseActionType.SendDigits;
    @JsonProperty(value = "Parameters")
    private Parameters parameters;

    // Set on ACTION_FAILED
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonProperty(value = "ErrorType", access = JsonProperty.Access.WRITE_ONLY)
    private String errorType;
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonProperty(value = "ErrorMessage", access = JsonProperty.Access.WRITE_ONLY)
    private String errorMessage;
    
    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    public static class Parameters implements Serializable {

        @JsonProperty(value = "CallId")
        private String callId;
        @JsonProperty(value = "Digits")
        private String digits;
        @JsonProperty(value = "ToneDurationInMilliseconds")
        private Integer toneDurationInMilliseconds;
    }
    
}
