package cloud.cleo.chimesma.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author sjensen
 */
@Data
@NoArgsConstructor
public class ActionDataCallUpdateRequest implements ResponseAction, Serializable {

    private final ResponseActionType type = ResponseActionType.CallUpdateRequest;
    

    @JsonProperty(value = "Parameters")
    private Parameters parameters;

    @Data
    @NoArgsConstructor
    public static class Parameters implements Serializable {

        @JsonProperty(value = "Arguments")
        private Map<String,String> Arguments;
     
    }

}
