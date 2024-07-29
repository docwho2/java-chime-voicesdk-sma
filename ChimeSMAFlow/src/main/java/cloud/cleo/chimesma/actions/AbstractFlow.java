/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import static cloud.cleo.chimesma.actions.ReceiveDigitsAction.RECEIVE_DIGITS_ID;

import cloud.cleo.chimesma.model.*;
import static cloud.cleo.chimesma.model.SMARequest.SMAEventType.*;
import cloud.cleo.chimesma.model.SMARequest.Status;
import static cloud.cleo.chimesma.model.SMARequest.Status.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author sjensen
 */
public abstract class AbstractFlow implements RequestStreamHandler {

    // Initialize the Log4j logger.
    protected final static Logger log = LogManager.getLogger(AbstractFlow.class);

    private final static ObjectMapper mapper = new ObjectMapper();

    // The first action in the call start
    private static Action startAction;

    // Default Error Handler when a Action doesn't specify one
    private static Action errorAction;

    // Map of action Ids
    private final static Map<Integer, Action> actions = new HashMap<>();

    private final static Set<Action> actSet = new HashSet<>();

    protected final static Map<Locale, ResponseSpeak.VoiceId> voice_map = new HashMap<>();

    private static volatile int idCounter = 1;

    public final static String CURRENT_ACTION_ID = "CurrentActionId";
    public final static String CURRENT_ACTION_ID_LIST = "CurrentActionIdList";

    protected AbstractFlow() {
        if (startAction == null) {
            log.debug("Starting to Build Static Flow");
            startAction = getInitialAction();
            log.debug("Initial Action is " + startAction.getDebugSummary());
        }

        if (errorAction == null) {
            errorAction = getErrorAction();
            if (errorAction == null) {
                // If the Flow didn't provide an action, then use a hangup
                errorAction = HangupAction.builder().withDescription("System Generated Error Action").build();
            }
            log.debug("Error Action is " + errorAction.getDebugSummary());
        }

        final var vmapStr = System.getenv("LANGUAGE_VOICE_MAP");
        if (vmapStr != null && voice_map.isEmpty()) {
            log.debug("Processing Locale to VoiceId mapping from ENV " + vmapStr);
            try {
                List<LocaleVoiceId> list = mapper.readerForListOf(LocaleVoiceId.class).readValue(vmapStr);
                for (var map : list) {
                    if (map.locale != null && map.voiceId != null) {
                        try {
                            log.debug("Adding Locale [" + map.locale + "] with VoiceId [" + map.voiceId + "]");
                            voice_map.put(Locale.forLanguageTag(map.locale), ResponseSpeak.VoiceId.valueOf(map.voiceId));
                        } catch (Exception e) {
                            log.error("Error processing Locale", e);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error processing Locale to Voice mappings from ENV", e);
            }
        }

        final var vmap = getLanguageToVoiceIdMap();
        if (vmap != null) {
            log.debug("Flow has provided Lang to VoiceId mapping, adding...");
            voice_map.putAll(vmap);
        }

    }

    /**
     * JSON Object for Locale and VoiceId placed in the Environment Example: [ { "Locale": "en-US", "VoiceId": "Joanna"
     * } , {"Locale": "es-US", "VoiceId": "Lupe" } ]
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class LocaleVoiceId {

        @JsonProperty(value = "Locale", required = true)
        private String locale;
        @JsonProperty(value = "VoiceId", required = true)
        private String voiceId;
    }

    protected synchronized final static Integer registerAction(Action action) {
        if (!actSet.contains(action)) {
            actSet.add(action);
            log.debug("Registering ID  " + idCounter + " as " + action.getClass().getSimpleName());
            actions.put(idCounter, action);
            return idCounter++;
        } else {
            return actions.entrySet().stream()
                    .filter((t) -> t.getValue().equals(action))
                    .map(t -> t.getKey()).findAny().orElse(0);
        }
    }

    protected abstract Action getInitialAction();

    protected abstract Action getErrorAction();

    protected abstract void newCallHandler(Action action);

    protected abstract void hangupHandler(Action action);

    protected Map<Locale, ResponseSpeak.VoiceId> getLanguageToVoiceIdMap() {
        return null;
    }

    private LinkedList<Action> getActions(Action initialAction, SMARequest event) throws CloneNotSupportedException {
        var list = new LinkedList<Action>();

        // Add the first action always
        var action = initialAction.clone(event);
        list.add(action);
        final var attrs = action.getTransactionAttributes();
        log.info("Adding action " + action.getDebugSummary());

        int counter = 1;  // Can only send max of 10 actions at a time
        while (action.isChainable() && action.getNextRoutingAction() != null && counter < 10) {
            // We have a next action
            final var nextAction = action.getNextRoutingAction().clone(event);
            list.add(nextAction);
            attrs.putAll(nextAction.getTransactionAttributes());
            log.info("Chaining action " + nextAction.getDebugSummary());
            action = nextAction;
            counter++;
        }

        // When we chain a bunch of actions, we'll need to also know the list of
        // ID's in order in case one errors in the middle of the list for example
        attrs.put(CURRENT_ACTION_ID_LIST,
                list.stream().map(a -> a.getId().toString()).collect(Collectors.joining(",")));

        // Always push out the current locale as it can change per action
        attrs.put("locale", list.getLast().getLocale().toLanguageTag());

        // The last Action will contain the summation of all the attrs
        list.getLast().setTransactionAttributes(attrs);
        return list;
    }

    private Action getCurrentAction(SMARequest event) throws CloneNotSupportedException {
        final var attrs = event.getCallDetails().getTransactionAttributes();

        String actionIdStr = null;
        switch (event.getInvocationEventType()) {
            case DIGITS_RECEIVED:
                actionIdStr = (String) attrs.get(RECEIVE_DIGITS_ID);
                break;
            case ACTION_FAILED:
                List<Integer> list = Arrays.stream(attrs.get(CURRENT_ACTION_ID_LIST).toString().split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                final var adType = event.getActionData().getType();
                if (list.size() > 1) {
                    // Traverse the list until we match the correct type
                    for (var id : list) {
                        final var a = actions.get(id);
                        if (adType.equals(a.getActionType())) {
                            actionIdStr = a.getId().toString();
                            break;
                        }
                    }
                    if (actionIdStr != null) {
                        break;
                    }
                }
            default:
                actionIdStr = (String) attrs.get(CURRENT_ACTION_ID);
        }

        final var actionId = Integer.valueOf(actionIdStr);
        final var action = actions.get(actionId).clone(event);
        log.debug("Current Action is " + action.getDebugSummary() + " with ID " + action.getId());
        return action;
    }

   
     @Override
    public void handleRequest(InputStream in, OutputStream out, Context cntxt) throws IOException {
         mapper.writeValue(out, handleRequest( mapper.readValue(in, SMARequest.class),cntxt ));
    }
    
    public final SMAResponse handleRequest(SMARequest event, Context cntxt) {
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
                    action.onActionSuccessful();
                    res = defaultResponse(action, event);
                    break;
                case DIGITS_RECEIVED:
                    action = getCurrentAction(event);
                    action.onActionSuccessful();
                    final var dr_attrs = event.getCallDetails().getTransactionAttributes();
                    final var dr_actionIdStr = (String) dr_attrs.get(CURRENT_ACTION_ID);
                    // We need to check if we were weren't the last action, because if we weren't then we need to get that action
                    // And set as current
                    final var dr_nra = action.getNextRoutingAction();
                    final var dr_List = getActions(dr_nra, event);
                    if (!action.getId().toString().equals(dr_actionIdStr)) {
                        // We weren't the current action ID, so reset to that
                        final var attrs_new = dr_List.getLast().getTransactionAttributes();
                        attrs_new.put(CURRENT_ACTION_ID, dr_actionIdStr);
                        res = SMAResponse.builder().withTransactionAttributes(attrs_new)
                                .withActions(dr_List.stream().map(a -> a.getResponse()).collect(Collectors.toList())).build();
                    } else {
                        res = defaultResponse(action, event);
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
                    log.error("Error for " + action.getDebugSummary());
                    if (event.getActionData() instanceof ErrorTypeMessage) {
                        final var ad = (ErrorTypeMessage) event.getActionData();
                        log.error("ErrorType = [" + ad.getErrorType() + "]");
                        log.error("ErrorMessage = [" + ad.getErrorMessage() + "]");
                    }
                    if (event.getActionData() instanceof ErrorMessage) {
                        final var ad = (ErrorMessage) event.getActionData();
                        log.error("Error = [" + ad.getError() + "]");
                    }
                    if (action.getErrorAction() != null) {
                        actionList = getActions(action.getErrorAction(), event);
                    } else {
                        // No error defined on the action itself, use flow error handler
                        actionList = getActions(errorAction, event);
                    }
                    res = SMAResponse.builder().withTransactionAttributes(actionList.getLast().getTransactionAttributes())
                            .withActions(actionList.stream().map(a -> a.getResponse()).collect(Collectors.toList())).build();
                    break;
                case HANGUP:
                    final var disconnectedBy = event.getCallDetails().getTransactionAttributes().getOrDefault("Disconnect", "Application");
                    log.debug("Call Was disconnected by [" + disconnectedBy + "], sending empty response");
                    action = getCurrentAction(event);
                    if (action instanceof CallAndBridgeAction) {
                        // Because Call Bridge has 2 call legs in play, delegate respone to the Action since there
                        // various way to handle things, but the default being once connected a hangup on one leg should drop the other
                        final var cab = (CallAndBridgeAction) action;
                        final var nextAction = cab.getHangupAction();
                        if (nextAction != null) {
                            actionList = getActions(nextAction, event);
                            res = SMAResponse.builder().withTransactionAttributes(actionList.getLast().getTransactionAttributes())
                                    .withActions(actionList.stream().map(a -> a.getResponse()).collect(Collectors.toList())).build();
                        } else {
                            // No next action after hangup
                            res = emptyResponse();
                        }
                    } else {
                        try {
                            hangupHandler(action);
                        } catch (Exception e) {
                            log.error("Exception in Hangup Handler", e);
                        }
                        res = emptyResponse();
                    }
                    break;
                case CALL_UPDATE_REQUESTED:
                    action = getCurrentAction(event);
                    final var newAction = callUpdateRequest(action, ((ActionDataCallUpdateRequest) event.getActionData()).getParameters().getArguments());
                    res = SMAResponse.builder().withTransactionAttributes(newAction.getTransactionAttributes())
                            .withActions(List.of(newAction.getResponse())).build();
                    break;
                case RINGING:
                    log.info("Outboud Call is RINGING (sending empty response)");
                    res = SMAResponse.builder().build();
                    break;
                case INVALID_LAMBDA_RESPONSE:
                    log.error(event.getInvocationEventType());
                    log.error("ErrorType = [" + event.getErrorType() + "]");
                    log.error("ErrorMessage = [" + event.getErrorMessage() + "]");
                default:
                    log.debug("Invocation type is unhandled, sending empty response for " + event.getInvocationEventType());
                    res = emptyResponse();
            }

            //log.debug(res);
            log.debug(mapper.valueToTree(res).toString());
            return res;
        } catch (RuntimeFailureException e) {
            log.error("In fail over mode, throwing back Exception to Lambda runtime");
            throw e;  // Singal to error this lambda
        } catch (Exception e) {
            log.error("Unhandled Exception", e);
            return SMAResponse.builder().withActions(List.of(ResponseHangup.builder().build())).build();
        }
    }

    protected Action callUpdateRequest(Action action, Map<String, String> arguments) {
        log.info("CallUpdateRequest handler, not overriden, returning null Action");
        return null;
    }

    private SMAResponse defaultResponse(Action action, SMARequest event) throws CloneNotSupportedException {
        SMAResponse res;
        if (event.getInvocationEventType().equals(ACTION_SUCCESSFUL) && Disconnected.equals(event.getCallDetails().getParticipants().get(0).getStatus())) {
            // We are just getting a success on the last Action while caller hung up, so we can't go to next action
            log.debug("Call is Disconnected on ACTION_SUCCESSFUL, so empty response");
            res = SMAResponse.builder().build();
        } else if (action.getNextRoutingAction() != null) {
            final var actionList = getActions(action.getNextRoutingAction(), event);
            res = SMAResponse.builder().withTransactionAttributes(actionList.getLast().getTransactionAttributes())
                    .withActions(actionList.stream().map(a -> a.getResponse()).collect(Collectors.toList())).build();
            log.info("Moving to next action: " + actionList.getFirst().getDebugSummary());
        } else if (action.getNextRoutingAction() == null && action instanceof CallAndBridgeAction) {
            //  When a call is bridged successfully, there is no action to take, and we don't want to hang up any legs
            log.debug("CallAndBridge no next step, so empty response");
            res = emptyResponse();
        } else {
            // If no action next, then end with hang up
            log.info("No next action in flow, ending call with hangup");
            res = hangupLegA();
        }
        return res;
    }

    private SMAResponse emptyResponse() {
        return SMAResponse.builder().build();
    }

    /**
     * Hangup LEG-A of the call (inbound call)
     *
     * @return
     */
    private SMAResponse hangupLegA() {
        return SMAResponse.builder().withActions(List.of(ResponseHangup.builder().withParameters(ResponseHangup.Parameters.builder().withParticipantTag(ParticipantTag.LEG_A).build()).build())).build();
    }

    /**
     * Hangup LEG-B of the call (inbound call)
     *
     * @return
     */
    private SMAResponse hangupLegB() {
        return SMAResponse.builder().withActions(List.of(ResponseHangup.builder().withParameters(ResponseHangup.Parameters.builder().withParticipantTag(ParticipantTag.LEG_B).build()).build())).build();
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
