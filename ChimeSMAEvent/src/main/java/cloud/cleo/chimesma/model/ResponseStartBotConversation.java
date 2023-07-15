/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.model;

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
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ResponseStartBotConversation implements ResponseAction, ErrorTypeMessage, Serializable {

    private final ResponseActionType type = ResponseActionType.StartBotConversation;
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

        @JsonProperty(value = "CallId")
        private String callId;
        
        @JsonProperty(value = "ParticipantTag")
        @Builder.Default
        private ParticipantTag participantTag = ParticipantTag.LEG_A;
        
        @JsonProperty(value = "BotAliasArn")
        @Builder.Default
        private String botAliasArn = System.getenv("BOT_ALIAS_ARN");
        
        @JsonProperty(value = "LocaleId")
        private String localeId;
        
        @JsonProperty(value = "Configuration")
        private Configuration configuration;

    }

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    public static class Configuration implements Serializable {

        @JsonProperty(value = "SessionState")
        private SessionState sessionState;

        @JsonProperty(value = "WelcomeMessages")
        private List<WelcomeMessage> welcomeMessages;
    }

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    public static class SessionState implements Serializable {

        @JsonProperty(value = "SessionAttributes")
        private Map<String, String> sessionAttributes;
        
        @JsonProperty(value = "DialogAction")
        private DialogAction dialogAction;

        // TODO Figure out how to send Intent with with Delegate Dialog Action (no syntax example in docs)
        @JsonProperty(value = "Intent")
        private Intent intent;

    }

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    public static class DialogAction implements Serializable {

        @JsonProperty(value = "Type")
        @Builder.Default
        private DialogActionType type = DialogActionType.ElicitIntent;
    }

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    public static class WelcomeMessage implements Serializable {

        @JsonProperty(value = "Content")
        private String content;
        @JsonProperty(value = "ContentType")
        @Builder.Default
        private TextType contentType = TextType.PlainText;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    public static class Intent implements Serializable {

        @JsonProperty(value = "Name")
        private String name;

        @JsonProperty(value = "Slots")
        private Map<String, Slot> Slots;

        @JsonProperty(value = "State")
        private IntentState state;

        @JsonProperty(value = "ConfirmationState")
        private ConfirmationState confirmationState;

    }
    
    public enum IntentState {
        /**
         * The Lambda function failed to fulfill the intent.
         */
        Failed,
        /**
         * The Lambda function fulfilled the intent.
         */
        Fulfilled,
        /**
         * The information for the intent is present, and your Lambda function can fulfill the intent.
         */
        ReadyForFulfillment
    }
    
    public enum ConfirmationState {
        /**
         * The Intent is fulfilled.
         */
        Confirmed,
        /**
         * The user responded "no" to the confirmation prompt.
         */
        Denied,
        /**
         * The user wasn't prompted for confirmation, or the user was prompted but didn't confirm or deny the prompt.
         */
        None
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    public static class Slot implements Serializable {

        @JsonProperty(value = "Value")
        private SlotValue value;
        @JsonProperty(value = "Values")
        private Slot[] values;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    public static class SlotValue implements Serializable {

        @JsonProperty(value = "InterpretedValue")
        private String interpretedValue;
        @JsonProperty(value = "OriginalValue")
        private String originalValue;
        @JsonProperty(value = "ResolvedValues")
        private List<String> resolvedValues;
    }

    public enum DialogActionType {
        Delegate,
        ElicitIntent
    }

    public enum TextType {
        PlainText,
        SSML
    }
}
