package cloud.cleo.chimesma.model;

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
public  class ResponseStopCallRecording implements ResponseAction, Serializable {

    private final ResponseActionType type = ResponseActionType.StopCallRecording;
    
    @JsonProperty(value = "Parameters")
    private Parameters parameters;

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameters implements Serializable {

        @JsonProperty(value = "CallId")
        private String callId;
        
    }
}
