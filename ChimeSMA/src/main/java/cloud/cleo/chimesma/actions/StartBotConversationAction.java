/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.*;
import cloud.cleo.chimesma.model.ResponseStartBotConversation.*;
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
public class StartBotConversationAction extends Action<StartBotConversationAction> {

    protected ParticipantTag participantTag;
    protected String botAliasArn = System.getenv("BOT_ALIAS_ARN");

    protected Map<String, String> sessionAttributes;

    protected DialogActionType dialogActionType = ResponseStartBotConversation.DialogActionType.ElicitIntent;

    protected String content;
    protected Function<StartBotConversationAction, String> contentFunction;

    protected TextType contentType = TextType.PlainText;

    @Override
    public ResponseAction getResponse() {

        String myContent = null;
        if (content != null || contentFunction != null) {
            myContent = contentFunction != null ? contentFunction.apply(this) : content;
        }

        WelcomeMessage welcome = null;
        if (myContent != null) {
            welcome = ResponseStartBotConversation.WelcomeMessage.builder()
                    .withContent(myContent)
                    .withContentType(Action.getBotContentType(myContent))
                    .build();
            // When a welcome message is set, Dialog Action must be set to IllicitIntent
            dialogActionType = DialogActionType.ElicitIntent;
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

        final var config = ResponseStartBotConversation.Configuration.builder()
                .withSessionState(ss)
                .withWelcomeMessages(welcome != null ? List.of(welcome) : null)
                .build();

        final var params = ResponseStartBotConversation.Parameters.builder()
                .withCallId(callId)
                .withParticipantTag(participantTag)
                .withBotAliasArn(botAliasArn)
                .withConfiguration(config)
                // Bots use Java syntax with underscore and not tag format with dash
                .withLocaleId(getLocale().toString())
                .build();
        return ResponseStartBotConversation.builder().withParameters(params).build();
    }

    public String getIntent() {
        String intent = "NULL";
        try {
            final var ad = getEvent().getActionData();
            if ( ad instanceof ActionDataStartBotConversation ) {
                intent = ((ActionDataStartBotConversation) ad).getIntentResult()
                        .getSessionState().getIntent().getName();
            }
        } catch (Exception e) {
            log.error("Error getting Intent from Event reponse", e);
        }
        return intent;
    }

    @Override
    protected StringBuilder getDebugSummary() {
        final var sb = super.getDebugSummary();

        if (dialogActionType != null) {
            sb.append(" da=[").append(getDialogActionType()).append(']');
        }

        if (contentFunction != null) {
            // Guard against function erroring
            try {
                sb.append(" textF=[").append(contentFunction.apply(this)).append(']');
            } catch (Exception e) {
                log.error(this.getClass() + " function error", e);
            }
        } else if (content != null) {
            sb.append(" text=[").append(getContent()).append(']');
        }

        return sb;

    }

    public static StartBotConversationActionBuilder builder() {
        return new StartBotConversationActionBuilder();
    }

    @NoArgsConstructor
    public static class StartBotConversationActionBuilder extends ActionBuilder<StartBotConversationActionBuilder, StartBotConversationAction> {

        private ParticipantTag participantTag;
        private String botAliasArn = System.getenv("BOT_ALIAS_ARN");
        private Map<String, String> sessionAttributes;
        private DialogActionType dialogActionType = ResponseStartBotConversation.DialogActionType.ElicitIntent;
        private String content;
        private Function<StartBotConversationAction, String> contentFunction;
        private TextType contentType = TextType.PlainText;

        public StartBotConversationActionBuilder withParticipantTag(ParticipantTag value) {
            this.participantTag = value;
            return this;
        }

        public StartBotConversationActionBuilder withBotAliasArn(String value) {
            this.botAliasArn = value;
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

        @Override
        protected StartBotConversationAction buildImpl() {
            return new StartBotConversationAction(participantTag, botAliasArn, sessionAttributes, dialogActionType, content, contentFunction, contentType);
        }
    }

    /**
     * Bot results need to be checked and SMA won't allow actions after when sending back multiple actions
     *
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
