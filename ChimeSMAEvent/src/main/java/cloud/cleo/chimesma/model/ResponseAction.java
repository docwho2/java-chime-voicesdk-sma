package cloud.cleo.chimesma.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author sjensen
 */
public interface ResponseAction {

    @JsonProperty(value = "Type")
    public ResponseActionType getType();

}
