/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.model;

import cloud.cleo.chimesma.model.ResponsePlayAudio.AudioSource;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
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
public class ResponseCallAndBridge implements ResponseAction, ErrorTypeMessage, Serializable {

    private final ResponseActionType type = ResponseActionType.CallAndBridge;

    /**
     * This is call and bridge doc
     */
    @JsonProperty(value = "Parameters")
    private Parameters parameters;
    
    // Set on ACTION_FAILED
    private String errorType;
    private String errorMessage;

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    public static class Parameters implements Serializable {

        @JsonProperty(value = "CallTimeoutSeconds")
        private Integer callTimeoutSeconds;
        @JsonProperty(value = "CallerIdNumber")
        private String callerIdNumber;

        @JsonProperty(value = "RingbackTone")
        private AudioSource ringbackTone;

        @JsonProperty(value = "Endpoints")
        private List<Endpoint> endpoints;
        @JsonProperty(value = "SipHeaders")
        private Map<String, String> sipHeaders;

    }

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    public static class Endpoint implements Serializable {

        @JsonProperty(value = "BridgeEndpointType")
        private BridgeEndpointType bridgeEndpointType;
        @JsonProperty(value = "Arn")
        private String arn;
        @JsonProperty(value = "Uri")
        private String uri;
    }

    public enum BridgeEndpointType {
        PSTN,
        AWS
    }
}
