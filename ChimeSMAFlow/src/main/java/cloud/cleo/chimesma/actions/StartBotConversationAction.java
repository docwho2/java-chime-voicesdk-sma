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
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 *
 * @author sjensen
 */
@Data
@SuperBuilder(setterPrefix = "with")
public class StartBotConversationAction extends Action<StartBotConversationAction, ActionDataStartBotConversation> {

    protected ParticipantTag participantTag;

    @Builder.Default
    protected String botAliasArn = System.getenv("BOT_ALIAS_ARN");
    protected Function<StartBotConversationAction, String> botAliasArnF;

    protected Map<String, String> sessionAttributes;
    protected Function<StartBotConversationAction, Map<String, String>> sessionAttributesF;

    @Builder.Default
    protected DialogActionType dialogActionType = ResponseStartBotConversation.DialogActionType.ElicitIntent;

    protected String content;
    protected Function<StartBotConversationAction, String> contentF;

    @Override
    protected ResponseAction getResponse() {

        String myContent = getFuncValOrDefault(contentF, content);

        WelcomeMessage welcome = null;
        if (myContent != null) {
            welcome = ResponseStartBotConversation.WelcomeMessage.builder()
                    .withContent(myContent)
                    .withContentType(getBotContentType(myContent))
                    .build();
            // When a welcome message is set, Dialog Action must be set to IllicitIntent
            dialogActionType = DialogActionType.ElicitIntent;
        }

        DialogAction da = null;
        if (dialogActionType != null) {
            da = DialogAction.builder().withType(dialogActionType).build();
        }

        SessionState ss = null;
        if (getFuncValOrDefault(sessionAttributesF, sessionAttributes) != null || da != null) {
            ss = SessionState.builder()
                    .withDialogAction(da)
                    .withSessionAttributes(getFuncValOrDefault(sessionAttributesF, sessionAttributes))
                    .build();
        }

        final var config = ResponseStartBotConversation.Configuration.builder()
                .withSessionState(ss)
                .withWelcomeMessages(welcome != null ? List.of(welcome) : null)
                .build();

        final var params = ResponseStartBotConversation.Parameters.builder()
                .withCallId(getCallId())
                .withParticipantTag(participantTag)
                .withBotAliasArn(getFuncValOrDefault(botAliasArnF, botAliasArn))
                .withConfiguration(config)
                // Bots use Java syntax with underscore and not tag format with dash
                .withLocaleId(getLocale().toString())
                .build();
        return ResponseStartBotConversation.builder().withParameters(params).build();
    }

    public String getIntentName() {
        try {
            final var ad = getEvent().getActionData();
            if (ad instanceof ActionDataStartBotConversation) {
                return ((ActionDataStartBotConversation) ad).getIntentResult()
                        .getSessionState().getIntent().getName();
            }
        } catch (Exception e) {
            log.error("Error getting Intent from Event reponse", e);
        }
        return "NULL";
    }
    

    @Override
    protected void onActionSuccessful() {
        log.debug("Lex Bot has finished and Intent is " + getIntentName());
        setTransactionAttribute("LexLastMatchedIntent", getIntentName());
    }

    @Override
    protected StringBuilder getDebugSummary() {
        final var sb = super.getDebugSummary();

        if (dialogActionType != null) {
            sb.append(" da=[").append(getDialogActionType()).append(']');
        }

        if (getFuncValOrDefault(contentF, content) != null) {
            sb.append(" content=[").append(getFuncValOrDefault(contentF, content)).append(']');
        }

        return sb;

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
    
    /**
     * Given message content, determine if the message is SSML or just plain text
     *
     * @param message
     * @return
     */
    private static ResponseStartBotConversation.TextType getBotContentType(String message) {
        if (message != null) {
            return message.toLowerCase().contains("<speak>") ? ResponseStartBotConversation.TextType.SSML : ResponseStartBotConversation.TextType.PlainText;
        }
        return ResponseStartBotConversation.TextType.PlainText;
    }

}
