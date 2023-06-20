/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.model;

import cloud.cleo.chimesma.ParticipantTag;
import com.amazonaws.services.lambda.runtime.events.LexV2Event.SessionState;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.polly.model.TextType;

/**
 *
 * @author sjensen
 */
@Data
@Builder(setterPrefix = "with")
@NoArgsConstructor
@AllArgsConstructor
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
        public static class Configuration implements Serializable {

            @JsonProperty(value = "SessionState")
            private SessionState sessionState;

            @JsonProperty(value = "WelcomeMessages")
            private WelcomeMessage welcomeMessages;

            @Data
            @Builder(setterPrefix = "with")
            @NoArgsConstructor
            @AllArgsConstructor
            public static class WelcomeMessage implements Serializable {

                @JsonProperty(value = "Content")
                private String content;
                @JsonProperty(value = "ContentType")
                @JsonSerialize(using = ResponseSpeak.TextTypeSerializer.class)
                private TextType contentType;
            }

        }
    }

}
