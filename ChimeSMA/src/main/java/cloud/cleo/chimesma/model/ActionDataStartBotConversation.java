/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.model;

import cloud.cleo.chimesma.model.ResponseStartBotConversation.DialogActionType;
import cloud.cleo.chimesma.model.ResponseStartBotConversation.TextType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author sjensen
 */
@Data
@NoArgsConstructor
public class ActionDataStartBotConversation implements ResponseAction, Serializable {

    private final ResponseActionType type = ResponseActionType.StartBotConversation;

    @JsonProperty(value = "IntentResult")
    IntentResult intentResult;

    @JsonProperty(value = "Parameters")
    private Parameters parameters;

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
    public static class SessionState implements Serializable {

        @JsonProperty(value = "SessionAttributes")
        private Map<String, String> sessionAttributes;

        @JsonProperty(value = "Intent")
        private Intent intent;

    }

    @Data
    @NoArgsConstructor
    public static class Intent implements Serializable {

        @JsonProperty(value = "Name")
        private String name;

        @JsonProperty(value = "Slots")
        private Map<String, Slot> Slots;

        @JsonProperty(value = "State")
        private String state;

        @JsonProperty(value = "ConfirmationState")
        private String confirmationState;

    }

    @Data
    @NoArgsConstructor
    public static class Slot implements Serializable {

        @JsonProperty(value = "Value")
        private SlotValue value;
        @JsonProperty(value = "Values")
        private Slot[] values;
    }

    @Data
    @NoArgsConstructor
    public static class SlotValue implements Serializable {

        @JsonProperty(value = "InterpretedValue")
        private String interpretedValue;
        @JsonProperty(value = "OriginalValue")
        private String originalValue;
        @JsonProperty(value = "ResolvedValues")
        private List<String> resolvedValues;
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

    @Data
    @NoArgsConstructor
    public static class Parameters implements Serializable {

        @JsonProperty(value = "CallId")
        private String callId;
        @JsonProperty(value = "ParticipantTag")
        private ParticipantTag participantTag;

        @JsonProperty(value = "BotAliasArn")
        private String botAliasArn;
        @JsonProperty(value = "LocaleId")
        private String localeId;
        @JsonProperty(value = "Configuration")
        private Configuration configuration;

        @Data
        @NoArgsConstructor
        public static class Configuration implements Serializable {

            @JsonProperty(value = "SessionState")
            private SessionState sessionState;

            @Data
            @NoArgsConstructor
            public static class SessionState implements Serializable {

                @JsonProperty(value = "SessionAttributes")
                private Map<String, String> sessionAttributes;
                @JsonProperty(value = "DialogAction")
                private DialogAction dialogAction;

                @Data
                @NoArgsConstructor
                public static class DialogAction implements Serializable {

                    @JsonProperty(value = "Type")
                    private DialogActionType type;
                }

            }

            @JsonProperty(value = "WelcomeMessages")
            private List<WelcomeMessage> welcomeMessages;

            @Data
            @NoArgsConstructor
            public static class WelcomeMessage implements Serializable {

                @JsonProperty(value = "Content")
                private String content;
                @JsonProperty(value = "ContentType")
                private TextType contentType;
            }

        }
    }
}
