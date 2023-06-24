/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.model;

import cloud.cleo.chimesma.actions.ParticipantTag;
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
public class ResponseStartBotConversation implements ResponseAction, Serializable {

    private final ResponseActionType type = ResponseActionType.StartBotConversation;
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

        @JsonProperty(value = "BotAliasArn")
        @Builder.Default
        private String botAliasArn = System.getenv("BOT_ALIAS_ARN");
        @JsonProperty(value = "LocaleId")
        private String localeId;
        @JsonProperty(value = "Configuration")
        private Configuration configuration;

        @Data
        @Builder(setterPrefix = "with")
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(value = JsonInclude.Include.NON_NULL)
        public static class Configuration implements Serializable {

            @JsonProperty(value = "SessionState")
            private SessionState sessionState;

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

            }

            @JsonProperty(value = "WelcomeMessages")
            private List<WelcomeMessage> welcomeMessages;

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

        }
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
