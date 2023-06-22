/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma;

import cloud.cleo.chimesma.model.ResponseAction;
import cloud.cleo.chimesma.model.ResponseActionType;
import cloud.cleo.chimesma.model.ResponseStartBotConversation;
import cloud.cleo.chimesma.model.ResponseStartBotConversation.DialogActionType;
import cloud.cleo.chimesma.model.ResponseStartBotConversation.Parameters.Configuration.SessionState;
import cloud.cleo.chimesma.model.ResponseStartBotConversation.Parameters.Configuration.SessionState.DialogAction;
import cloud.cleo.chimesma.model.ResponseStartBotConversation.Parameters.Configuration.WelcomeMessage;
import cloud.cleo.chimesma.model.ResponseStartBotConversation.TextType;
import com.amazonaws.services.lambda.serialization.JacksonPojoSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author sjensen
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartBotConversationAction extends Action {

    private ParticipantTag participantTag;
    private String botAliasArn = System.getenv("BOT_ALIAS_ARN");
    private String localeId;

    private Map<String, String> sessionAttributes;

    private DialogActionType dialogActionType;

    private String content;
    private Function<StartBotConversationAction, String> contentFunction;
    
    private TextType contentType = TextType.PlainText;

    @JsonProperty(value = "IntentMatcher")
    private Function<StartBotConversationAction, Action> intentMatcher;

    @Override
    public ResponseAction getResponse() {

        WelcomeMessage welcome = null;
        if (content != null || contentFunction != null) {
            welcome = ResponseStartBotConversation.Parameters.Configuration.WelcomeMessage.builder()
                    .withContent(contentFunction != null ? contentFunction.apply(this) : content)
                    .withContentType(contentType)
                    .build();
        }

        DialogAction da = null;
        if (dialogActionType != null) {
            da = DialogAction.builder().withType(dialogActionType).build();
        }

        SessionState ss = null;
        if (sessionAttributes != null || da != null) {
            ss = SessionState.builder()
                    .withDialogAction(da)
                    .withSessionAttributes(sessionAttributes)
                    .build();
        }

        final var config = ResponseStartBotConversation.Parameters.Configuration.builder()
                .withSessionState(ss)
                .withWelcomeMessages(welcome != null ? List.of(welcome) : null)
                .build();

        final var params = ResponseStartBotConversation.Parameters.builder()
                .withCallId(callId)
                .withParticipantTag(participantTag)
                .withBotAliasArn(botAliasArn)
                .withConfiguration(config)
                .withLocaleId(localeId)
                .build();
        return ResponseStartBotConversation.builder().withParameters(params).build();
    }

    public String getIntent() {
        JsonNode json = JacksonPojoSerializer.getInstance().getMapper().valueToTree(getEvent().getActionData().get("IntentResult"));
        return json.findValue("SessionState").findValue("Intent").findValue("Name").asText();
    }
    
    
    @Override
    protected StringBuilder getDebugSummary() {
        return super.getDebugSummary()
                .append(" [").append(getBotAliasArn()).append(']');
    }

    public static StartBotConversationActionBuilder builder() {
        return new StartBotConversationActionBuilder();
    }

    @NoArgsConstructor
    public static class StartBotConversationActionBuilder extends ActionBuilder<StartBotConversationActionBuilder, StartBotConversationAction> {

        private ParticipantTag participantTag;
        private String botAliasArn = System.getenv("BOT_ALIAS_ARN");
        private String localeId;
        private Map<String, String> sessionAttributes;
        private DialogActionType dialogActionType;
        private String content;
        private Function<StartBotConversationAction, String> contentFunction;
        private TextType contentType = TextType.PlainText;

        private Function<StartBotConversationAction, Action> intentMatcher;

        public StartBotConversationActionBuilder withParticipantTag(ParticipantTag value) {
            this.participantTag = value;
            return this;
        }

        public StartBotConversationActionBuilder withBotAliasArn(String value) {
            this.botAliasArn = value;
            return this;
        }

        public StartBotConversationActionBuilder withLocaleId(String value) {
            this.localeId = value;
            return this;
        }

        public StartBotConversationActionBuilder withSessionAttributes(Map<String, String> value) {
            this.sessionAttributes = value;
            return this;
        }

        public StartBotConversationActionBuilder withDialogActionType(DialogActionType value) {
            this.dialogActionType = value;
            return this;
        }

        public StartBotConversationActionBuilder withContent(String value) {
            this.content = value;
            return this;
        }
        
        public StartBotConversationActionBuilder withContent(Function<StartBotConversationAction, String> value) {
            this.contentFunction = value;
            return this;
        }

        public StartBotConversationActionBuilder withContentType(TextType value) {
            this.contentType = value;
            return this;
        }
        
        public StartBotConversationActionBuilder withIntentMatcher(Function<StartBotConversationAction, Action> value) {
            this.intentMatcher = value;
            return this;
        }

        @Override
        protected StartBotConversationAction buildImpl() {
            return new StartBotConversationAction(participantTag, botAliasArn, localeId, sessionAttributes, dialogActionType, content, contentFunction, contentType, intentMatcher);
        }
    }
    
    /**
     * Bot results need to be checked and SMA won't allow actions after when sending back multiple actions
     * @return
     */
    @Override
    protected boolean isChainable() {
        return false;
    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.StartBotConversation;
    }

}
