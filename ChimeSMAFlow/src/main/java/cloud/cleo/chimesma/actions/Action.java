/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import static cloud.cleo.chimesma.actions.AbstractFlow.CURRENT_ACTION_ID;
import cloud.cleo.chimesma.model.*;
import static cloud.cleo.chimesma.model.SMARequest.SMAEventType.ACTION_FAILED;
import com.amazonaws.services.lambda.serialization.JacksonPojoSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Builder;
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
 * @param <R> The type that is returned in the ActionData for responses back from SMA
 */
@Data
@SuperBuilder(setterPrefix = "with")
@NoArgsConstructor
public abstract class Action<A extends Action, R extends ResponseAction> implements Cloneable {

    protected final static Logger log = LogManager.getLogger(Action.class);

    protected final static ObjectMapper mapper = JacksonPojoSerializer.getInstance().getMapper();

    // Id used to track unique Java Object Actions
    private final Integer id = AbstractFlow.registerAction(this);

    /**
     * Short description for this Action that will be logged in the log file. Use something short but descriptive that
     * will help you when viewing log files.
     */
    private String description;

    /**
     * Call ID used to track call Legs
     */
    @Setter(AccessLevel.PROTECTED)
    private String callId;

    private Action nextAction;
    private Function<A, Action> nextActionF;
    
    private Action errorAction;

    @Setter(AccessLevel.PROTECTED)
    private SMARequest event;

    // Always maintain a Language
    private Locale locale;


    @Builder.Default
    private Map<String, Object> transactionAttributes = new HashMap<>();

    protected abstract ResponseAction getResponse();

    /**
     * The Action Data from SMA Request if the type matches,
     * otherwise null.  When callers get null, they should
     * then check the type, cast, and then use the the object.
     * Getting null indicates ActionData was generated from another Action.
     * Example, CallAndBridge might receive Hangup ActionData.
     *
     * @return
     */
    public final R getActionData() {
        if (event.getActionData().getType().equals(getActionType())) {
            return (R) event.getActionData();
        }
        return null;
    }
    
    /**
     * If this Action resulted in an error, get the associated error 
     * messages returned.
     * 
     * @return error string or empty string if no error or message
     */
    public String getErrorMessage() {
        if ( event.getInvocationEventType().equals(ACTION_FAILED) ) {
           if ( this instanceof ErrorMessage ) {
               return ((ErrorMessage )event.getActionData()).getError();
           }
           if ( this instanceof ErrorTypeMessage ) {
               final var eMesg = (ErrorTypeMessage )event.getActionData();
               return eMesg.getErrorType() + " : " + eMesg.getErrorMessage();
           }
        }
        return "";
    }

    public abstract ResponseActionType getActionType();

    protected boolean isChainable() {
        return true;
    }

    /**
     * Given a function execute and return the function value if function is not null. Return value if the function
     * throws an exception or is null. Note the value could be null as well.
     *
     * @param <V>
     * @param function
     * @param value
     * @return
     */
    protected final <V> V getFuncValOrDefault(Function<A, V> function, V value) {
        if (function != null) {
            try {
                return function.apply((A) this);
            } catch ( RuntimeFailureException rfe ) {
                throw rfe;
            } catch (Exception e) {
                log.error(getClass().getSimpleName() + " Function Exception",e);
            }
        }
        // If no function or function threw exception, return just the value
        return value;
    }

    /**
     * Gets the next routing Action to continue the flow. Sub classes can override to provide different logic for going
     * to the next action.
     *
     * @return the next action
     */
    protected Action getNextRoutingAction() {
        return getFuncValOrDefault(nextActionF, nextAction);
    }

    /**
     * Generate a short and concise debug line that is readable in the logs. Sub classes should always call super() on
     * this and add what is relevant.
     *
     * @return summary string targeted for logging
     */
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
        clone.setTransactionAttributes(event.getCallDetails().getTransactionAttributes() == null ? new HashMap<>()
                : event.getCallDetails().getTransactionAttributes());

        // Always set our ID
        clone.setTransactionAttribute(CURRENT_ACTION_ID, getId().toString());

        // Always set our lang (use language tags as that is consistant for in and out)
        //  Bots take Java form with _ and Speak actions take it with - (but for us, Java Locale object can output both)
        if ( clone.getLocale() == null ) {
            // No Locale set on this action, so pull from attrs
            //log.debug("This Action has no locale set, getting from attrs or default");
            clone.setLocale(Locale.forLanguageTag(clone.getTransactionAttributes().getOrDefault("locale", "en-US").toString()));
        } else {
            log.debug("This Action has a locale set to " + clone.getLocale());
        }
        

        // Make Call Event associated with this Action available
        clone.setEvent(event);

        return clone;
    }

    /**
     * Called when ACTION_SUCCESSFUL happens on a action. Sub classes should override to add logic for success events,
     * like call recording to store the file name.
     *
     */
    protected void onActionSuccessful() {

    }
    

    public Map<String, Object> setTransactionAttribute(String key, Object object) {
        final var ta = getTransactionAttributes();
        ta.put(key, object);
        return ta;
    }

    public Object getTransactionAttribute(String key) {
        return getTransactionAttributes().get(key);
    }

    public Object getTransactionAttributeOrDefault(String key, Object defaultValue) {
        return getTransactionAttributes().getOrDefault(key, defaultValue);
    }

    protected String getRecievedDigitsFromAction() {
        final var ad = getEvent().getActionData();
        if (ad instanceof ReceivedDigits) {
            final var rd =  ((ReceivedDigits) ad).getReceivedDigits();
            return rd == null ? "" : rd;
        }
        return "";
    }

}
