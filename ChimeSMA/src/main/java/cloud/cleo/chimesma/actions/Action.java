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
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author sjensen
 * @param <A>
 * @param <R>  The type that is returned in the ActionData for responses back from SMA
 */
@Data
@SuperBuilder(setterPrefix = "with")
@NoArgsConstructor
public abstract class Action<A extends Action, R extends ResponseAction> implements Cloneable {

    protected final static Logger log = LogManager.getLogger();

    protected final static ObjectMapper mapper = JacksonPojoSerializer.getInstance().getMapper();

    // Id used to track unique Java Object Actions
    private final Integer id = AbstractFlow.registerAction(this);

    // Description to use in debug logs
    private String description;

    /**
     * Call ID used to track call Legs
     */
    @Setter(AccessLevel.PROTECTED)
    private String callId;
    private Action nextAction;
    private Function<A, Action> nextActionFunction;

    @Setter(AccessLevel.PROTECTED)
    private SMARequest event;

    // Always maintain a Language
    private Locale locale;

    @Setter(AccessLevel.PROTECTED)
    private Map<String, Object> transactionAttributes;
    

    protected abstract ResponseAction getResponse();
    
    /**
     * The Action Data from SMA Response
     * @return 
     */
    public final R getActionData() {
        return (R) event.getActionData();
    }
    
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

    protected A clone(SMARequest event) throws CloneNotSupportedException {
        final var clone = (A) super.clone();

        // We should always have a CallId on first participant
        clone.setCallId(event.getCallDetails().getParticipants().get(0).getCallId());

        // On new calls incoming will be null, so we need to create
        clone.setTransactionAttributes( event.getCallDetails().getTransactionAttributes() == null ? new HashMap<>()
                : event.getCallDetails().getTransactionAttributes() );

        // Always set our ID
        clone.setTransactionAttribute(CURRENT_ACTION_ID, getId().toString());

        // Always set our locale (use language tags as that is consistant for in and out)
        //  Bots take Java form with _ and Speak actions take it with - (but for us, Java Locale object can output both)
        clone.setLocale( Locale.forLanguageTag(clone.getTransactionAttributes().getOrDefault("locale", "en-US").toString()) );
        clone.setTransactionAttribute("locale", clone.getLocale().toLanguageTag());

        // Make Call Event associated with this Action available
        clone.setEvent(event);

        return clone;
    }
    
    public Map<String, Object> setTransactionAttribute(String key,Object object) {
        final var ta = getTransactionAttributes();
        ta.put(key, object);
        return ta;
    }
    
    public Object getTransactionAttribute(String key) {
        return  getTransactionAttributes().get(key);
    }
    
     public Object getTransactionAttributeOrDefault(String key, Object defaultValue) {
        return  getTransactionAttributes().getOrDefault(key,defaultValue);
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
            return message.toLowerCase().contains("<speak>") ? ResponseStartBotConversation.TextType.SSML : ResponseStartBotConversation.TextType.PlainText;
        }
        return ResponseStartBotConversation.TextType.PlainText;
    }

    protected static ResponseSpeak.TextType getSpeakContentType(String message) {
        if (message != null) {
            return message.toLowerCase().contains("<speak>") ? ResponseSpeak.TextType.ssml : ResponseSpeak.TextType.text;
        }
        return ResponseSpeak.TextType.text;
    }

    
}
