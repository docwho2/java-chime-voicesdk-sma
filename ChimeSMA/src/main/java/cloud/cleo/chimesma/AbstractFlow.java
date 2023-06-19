/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma;

import static cloud.cleo.chimesma.ReceiveDigitsAction.RECEIVE_DIGITS_ID;
import static cloud.cleo.chimesma.SMAEvent.SMAEventType.DIGITS_RECEIVED;
import cloud.cleo.chimesma.SMAEvent.Status;
import cloud.cleo.chimesma.model.ResponseAction;
import cloud.cleo.chimesma.model.ResponseHangup;
import cloud.cleo.chimesma.model.SMAResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author sjensen
 */
public abstract class AbstractFlow implements RequestHandler<SMAEvent, SMAResponse> {

    // Initialize the Log4j logger.
    protected Logger log = LogManager.getLogger();

    // The first action in the call start
    private static Action startAction;

    // Default Error Handler when a Action doesn't specify one
    private static Action errorAction;

    // Map of action Ids
    private final static Map<Integer, Action> actions = new HashMap<>();

    private static final Set<Action> actSet = new HashSet<>();
    
    private static volatile int idCounter = 0;

    public final static String CURRENT_ACTION_ID = "CurrentActionId";

    public AbstractFlow() {
        if (startAction == null) {
            log.debug("Starting to Build Static Flow");
            startAction = getInitialAction();
            log.debug("Initial Action is " + startAction.getDebugSummary());
        }

        if (errorAction == null) {
            errorAction = getErrorAction();
            log.debug("Error Action is " + errorAction.getDebugSummary());
        }
    }

    protected synchronized  final static void registerAction(Action action) {
        if (!actSet.contains(action)) {
            actSet.add(action);
            action.setId(idCounter);
            actions.put(idCounter++, action);
        }
    }
    
    private void processActions(Action action, Integer id) {
        if (!actSet.contains(action)) {
            actSet.add(action);
            action.setId(id);
            actions.put(id, action);
            log.debug("Adding [" + action.getActionType() + " as " + action + " to Map with ID " + id);
            if (action.getNextAction() != null) {
                processActions(action.getNextAction(), ++id);
            }
        } else {
            log.debug("Action " + action + " already processed and has ID " + action.getId());
        }
    }

    protected abstract Action getInitialAction();

    protected abstract Action getErrorAction();

    protected abstract void newCallHandler(Action action);

    protected abstract void hangupHandler(Action action);

    private LinkedList<Action> getActions(Action initialAction, SMAEvent event) throws CloneNotSupportedException {
        var list = new LinkedList<Action>();

        // Add the first action always
        var action = initialAction.clone(event);
        list.add(action);
        final var attrs = action.getTransactionAttributes();
        log.debug("Adding action " + action.getDebugSummary());

        while (action.getNextAction() != null && action.isChainable()) {
            // We have a next action
            final var nextAction = action.getNextAction().clone(event);
            list.add(nextAction);
            attrs.putAll(nextAction.getTransactionAttributes());
            log.debug("Chaining action " + nextAction.getDebugSummary());
            action = nextAction;
        }
        // The last Action will contain the summation of all the attrs
        list.getLast().setTransactionAttributes(attrs);
        return list;
    }

    private Action getCurrentAction(SMAEvent event) throws CloneNotSupportedException {
        final var attrs = event.getCallDetails().getTransactionAttributes();
        //log.debug("Incoming Transaction Attributes");
        //attrs.forEach((k, v) -> log.debug("key=[" + k + "] value=[" + v + "]"));
        String actionIdStr;
        switch (event.getInvocationEventType()) {
            case DIGITS_RECEIVED:
                actionIdStr = (String) attrs.get(RECEIVE_DIGITS_ID);
                break;
            default:
                actionIdStr = (String) attrs.get(CURRENT_ACTION_ID);
        }

        final var actionId = Integer.valueOf(actionIdStr);
        final var action = actions.get(actionId).clone(event);
        log.debug("Current Action is " + action.getDebugSummary() + " with ID " + action.getId());
        return action;
    }

    @Override
    public final SMAResponse handleRequest(SMAEvent event, Context cntxt) {
        try {
            log.debug(event);

            SMAResponse res;
            switch (event.getInvocationEventType()) {
                case NEW_INBOUND_CALL:
                    // Start with the initial action
                    log.debug("New Inbound Call, starting flow");
                    var actionList = getActions(startAction, event);
                    res = SMAResponse.builder().withTransactionAttributes(actionList.getLast().getTransactionAttributes())
                            .withActions(actionList.stream().map(a -> a.getResponse()).collect(Collectors.toList())).build();
                    try {
                        newCallHandler(actionList.getLast());
                    } catch (Exception e) {
                        log.error("Exception in New Call Handler", e);
                    }
                    break;

                case ACTION_SUCCESSFUL:

                    var action = getCurrentAction(event);
                    if (action.getNextAction() != null) {
                        actionList = getActions(action.getNextAction(), event);
                        res = SMAResponse.builder().withTransactionAttributes(actionList.getLast().getTransactionAttributes())
                                .withActions(actionList.stream().map(a -> a.getResponse()).collect(Collectors.toList())).build();
                        log.debug("Moving to next action " + actionList.getFirst());
                    } else {
                        // If no action next, then end with hang up
                        log.debug("No next action in flow, ending call with hangup");
                        res = SMAResponse.builder().withActions(List.of(ResponseHangup.builder().build())).build();
                    }
                    break;
                case DIGITS_RECEIVED:
                    action = getCurrentAction(event);
                    if (action instanceof ReceiveDigitsAction) {
                        log.debug("Received Digits [" + ((ReceiveDigitsAction) action).getReceivedDigits() + "]");
                        actionList = getActions(((ReceiveDigitsAction) action).getDigitsRecevedAction(), event);
                        res = SMAResponse.builder().withTransactionAttributes(actionList.getLast().getTransactionAttributes())
                                .withActions(actionList.stream().map(a -> a.getResponse()).collect(Collectors.toList())).build();
                        log.debug("Moving to digits received action " + actionList.getFirst());
                    } else {
                        if (action.getNextAction() != null) {
                            actionList = getActions(action.getNextAction(), event);
                            res = SMAResponse.builder().withTransactionAttributes(actionList.getLast().getTransactionAttributes())
                                    .withActions(actionList.stream().map(a -> a.getResponse()).collect(Collectors.toList())).build();
                            log.debug("Moving to next action " + actionList.getFirst());
                        } else {
                            // If no action next, then end with hang up
                            log.debug("No next action in flow, ending call with hangup");
                            res = SMAResponse.builder().withActions(List.of(ResponseHangup.builder().build())).build();
                        }
                    }
                    break;
                case ACTION_FAILED:
                    action = getCurrentAction(event);
                    // First check for a disconnect on incoming call
                    final var participants = event.getCallDetails().getParticipants();
                    if (participants.size() == 1 && participants.get(0).getStatus().equals(Status.Disconnected)) {
                        log.debug("Call Was disconnected, sending empty response");
                        final var attrs = action.getTransactionAttributes();
                        attrs.put("Disconnect", "Caller");
                        res = SMAResponse.builder().withTransactionAttributes(attrs).build();
                        break;
                    }
                case HANGUP:
                    final var disconnectedBy = event.getCallDetails().getTransactionAttributes().getOrDefault("Disconnect", "Application");
                    log.debug("Call Was disconnected by [" + disconnectedBy + "], sending empty response");
                    action = getCurrentAction(event);
                    try {
                        hangupHandler(action);
                    } catch (Exception e) {
                        log.error("Exception in Hangup Handler", e);
                    }
                    res = SMAResponse.builder().build();
                    break;
                default:
                    log.debug("Invocation type is unhandled, sending empty response " + event.getInvocationEventType());
                    res = SMAResponse.builder().build();
            }

            log.debug(res);
            return res;
        } catch (Exception e) {
            log.error("Unhandled Exception", e);
            return SMAResponse.builder().withActions(List.of(ResponseHangup.builder().build())).build();
        }
    }

//    public void handleRequest(InputStream in, OutputStream out, Context cntxt) throws IOException {
//       // Read in JSON Tree
//        var json = mapper.readTree(in);
//        log.debug("INPUT JSON is " + json.toString());
//
//        final var res = handleRequest(JacksonPojoSerializer.getInstance().fromJson(json.toString(), SMAEvent.class), cntxt);
//        
//        try (Writer w = new OutputStreamWriter(out, "UTF-8")) {
//            w.write(mapper.writeValueAsString(res));
//        }
//    }
}
