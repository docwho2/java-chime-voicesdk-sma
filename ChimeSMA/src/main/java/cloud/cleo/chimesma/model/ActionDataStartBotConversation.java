/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.model;

import cloud.cleo.chimesma.model.ResponseStartBotConversation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author sjensen
 */
@Data
@NoArgsConstructor
public class ActionDataStartBotConversation implements ResponseAction, Serializable {

    @JsonProperty("CallId")
    private String callId;

    private final ResponseActionType type = ResponseActionType.StartBotConversation;

    @JsonProperty(value = "Parameters")
    private Parameters parameters;

    @JsonProperty(value = "IntentResult")
    IntentResult intentResult;

    // Set on ACTION_FAILED
    @JsonProperty("ErrorType")
    private String errorType;
    @JsonProperty("ErrorMessage")
    private String errorMessage;

    @Data
    @NoArgsConstructor
    public static class IntentResult implements Serializable {

        @JsonProperty(value = "SessionId")
        private String sessionId;

        @JsonProperty(value = "SessionState")
        private SessionState sessionState;

        @JsonProperty(value = "Interpretations")
        private List<Interpretation> interpretations;

    }


    @Data
    @NoArgsConstructor
    public static class Interpretation implements Serializable {

        @JsonProperty(value = "Intent")
        private Intent intent;
        
        @JsonProperty(value = "NluConfidence")
        private NluConfidence nluConfidence;
    }

    @Data
    @NoArgsConstructor
    public static class NluConfidence implements Serializable {

        @JsonProperty(value = "Score")
        private Double score;
    }

}
