/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma;

import static cloud.cleo.chimesma.AbstractFlow.CURRENT_ACTION_ID;
import cloud.cleo.chimesma.model.ResponseAction;
import cloud.cleo.chimesma.model.ResponseActionType;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

/**
 *
 * @author sjensen
 */
@Data
public abstract class Action implements Cloneable {

    // Id used to track unique Java Object Actions
    private Integer id;
    
    // Description to use in debug logs
    private String description;
    
    protected String callId;
    private Action nextAction;
    private SMAEvent event;
    

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
    
    protected StringBuilder getDebugSummary() {
        StringBuilder sb = new StringBuilder(getActionType().toString());
        if ( getDescription() != null ) {
            sb.append(" [").append(getDescription()).append(']');
        }
        return sb;
    }

    public Action clone(SMAEvent event) throws CloneNotSupportedException {
        final var clone = (Action) super.clone();
        
        // We should always have a CallId on first participant
        clone.callId = event.getCallDetails().getParticipants().get(0).getCallId();
        
        // On new calls incoming will be null, so we need to create
        clone.transactionAttributes = event.getCallDetails().getTransactionAttributes() == null ? new HashMap<>() :
                event.getCallDetails().getTransactionAttributes();
        
        // Always set our ID
        clone.transactionAttributes.put(CURRENT_ACTION_ID, getId().toString());
        
        // Make Call Event associated with this Action available
        clone.event = event;
        
        return clone;
    }

    protected static abstract class ActionBuilder<T extends ActionBuilder,F extends Action> {
        private String callId;
        private String description;
        private Action nextAction;
        

        public T withNextAction(Action nextAction) {
            this.nextAction = nextAction;
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
        
        public final F build() {
            final var impl = buildImpl();
            impl.setNextAction(nextAction);
            impl.setDescription(description);
            if ( callId != null ) {
                impl.setCallId(callId);
            }
            return impl;
        }
        
        protected abstract F buildImpl();
        
    }
}
