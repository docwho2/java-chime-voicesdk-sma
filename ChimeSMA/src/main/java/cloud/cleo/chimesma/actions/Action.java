/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import static cloud.cleo.chimesma.actions.AbstractFlow.CURRENT_ACTION_ID;
import cloud.cleo.chimesma.model.*;
import com.amazonaws.services.lambda.serialization.JacksonPojoSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author sjensen
 * @param <A>
 */
@Data
public abstract class Action<A extends Action> implements Cloneable {

    protected final static Logger log = LogManager.getLogger();

    protected final static ObjectMapper mapper = JacksonPojoSerializer.getInstance().getMapper();

    // Id used to track unique Java Object Actions
    private Integer id;

    // Description to use in debug logs
    protected String description;

    protected String callId;
    private Action nextAction;
    protected Function<A, Action> nextActionFunction;

    private SMARequest event;

    // Always maintain a Language
    protected Locale locale;

    protected Map<String, Object> transactionAttributes;

    public Action() {
        // Register all Actions
        AbstractFlow.registerAction(this);
    }

    public abstract ResponseAction getResponse();

    public abstract ResponseActionType getActionType();

    protected boolean isChainable() {
        return true;
    }
    

    protected Action getNextRoutingAction() {
        Action action;
        if (nextActionFunction != null) {
            try {
                action = nextActionFunction.apply((A) this);
            } catch (Exception e) {
                log.error(getDebugSummary(), e);
                log.info("Falling back to static next Acction due to exception");
                action = nextAction;
            }
        } else {
            action = nextAction;
        }

        return action;
    }

    protected StringBuilder getDebugSummary() {
        StringBuilder sb = new StringBuilder(getActionType().toString());
        if (getDescription() != null) {
            sb.append(" desc=[").append(getDescription()).append(']');
        }
        return sb;
    }

    public Action clone(SMARequest event) throws CloneNotSupportedException {
        final var clone = (Action) super.clone();

        // We should always have a CallId on first participant
        clone.callId = event.getCallDetails().getParticipants().get(0).getCallId();

        // On new calls incoming will be null, so we need to create
        clone.transactionAttributes = event.getCallDetails().getTransactionAttributes() == null ? new HashMap<>()
                : event.getCallDetails().getTransactionAttributes();

        // Always set our ID
        clone.transactionAttributes.put(CURRENT_ACTION_ID, getId().toString());

        // Always set our locale (use language tags as that is consistant for in and out)
        //  Bots take Java form with _ and Speak actions take it with - (but for us, Java Locale object can output both)
        clone.locale = Locale.forLanguageTag(clone.transactionAttributes.getOrDefault("locale", "en-US").toString());
        clone.transactionAttributes.put("locale", clone.locale.toLanguageTag());

        // Make Call Event associated with this Action available
        clone.event = event;

        return clone;
    }

    protected String getRecievedDigitsFromAction() {
        final var ad = getEvent().getActionData();
        if (ad instanceof ReceivedDigits ) {
            return ((ReceivedDigits) ad).getReceivedDigits();
        }
        return "";
    }

    /**
     * Given message content, determine if the message is SSML or just plain text
     *
     * @param message
     * @return
     */
    protected static ResponseStartBotConversation.TextType getBotContentType(String message) {
        if (message != null) {
            return message.toLowerCase().contains("<ssml>") ? ResponseStartBotConversation.TextType.SSML : ResponseStartBotConversation.TextType.PlainText;
        }
        return ResponseStartBotConversation.TextType.PlainText;
    }

    protected static ResponseSpeak.TextType getSpeakContentType(String message) {
        if (message != null) {
            return message.toLowerCase().contains("<ssml>") ? ResponseSpeak.TextType.ssml : ResponseSpeak.TextType.text;
        }
        return ResponseSpeak.TextType.text;
    }

    protected static abstract class ActionBuilder<T extends ActionBuilder, F extends Action> {

        private String callId;
        private String description;
        private Action nextAction;
        private Function<F, Action> nextActionFunction;
        private Locale locale;

        public T withNextAction(Action nextAction) {
            this.nextAction = nextAction;
            return (T) this;
        }

        public T withNextAction(Function<F, Action> nextActionFunction) {
            this.nextActionFunction = nextActionFunction;
            return (T) this;
        }

        public T withCallId(String callId) {
            this.callId = callId;
            return (T) this;
        }

        public T withDescription(String description) {
            this.description = description;
            return (T) this;
        }

        public T withLocale(Locale locale) {
            this.locale = locale;
            return (T) this;
        }

        public final F build() {
            final var impl = buildImpl();
            impl.setNextAction(nextAction);
            impl.setNextActionFunction(nextActionFunction);
            impl.setDescription(description);
            if (locale != null) {
                impl.setLocale(locale);
            }
            if (callId != null) {
                impl.setCallId(callId);
            }
            return impl;
        }

        protected abstract F buildImpl();

    }
}
