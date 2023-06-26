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
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.serialization.JacksonPojoSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public abstract class AbstractFlow implements RequestHandler<SMARequest, SMAResponse> {

    // Initialize the Log4j logger.
    protected final static Logger log = LogManager.getLogger();

    private final static ObjectMapper mapper = JacksonPojoSerializer.getInstance().getMapper();

    // The first action in the call start
    private static Action startAction;

    // Default Error Handler when a Action doesn't specify one
    private static Action errorAction;

    // Map of action Ids
    private final static Map<Integer, Action> actions = new HashMap<>();

    private final static Set<Action> actSet = new HashSet<>();

    protected final static Map<Locale, ResponseSpeak.VoiceId> voice_map = new HashMap<>();

    private static volatile int idCounter = 0;

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
            log.debug("Error Action is " + errorAction.getDebugSummary());
        }

        final var vmapStr = System.getenv("LANGUAGE_VOICE_MAP");
        if (vmapStr != null && voice_map.isEmpty()) {
            log.debug("Processing Locale to VoiceId mapping from ENV " + vmapStr);
            try {
                List<LocaleVoiceId> list = mapper.readerForListOf(LocaleVoiceId.class).readValue(vmapStr);
                for (var map : list) {
                    if (map.locale != null && map.voiceId != null) {
                        log.debug("Adding Locale [" + map.locale + "] with VoiceId [" + map.voiceId + "]");
                        voice_map.put(Locale.forLanguageTag(map.locale), ResponseSpeak.VoiceId.valueOf(map.voiceId));
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

    protected synchronized final static void registerAction(Action action) {
        if (!actSet.contains(action)) {
            actSet.add(action);
            action.setId(idCounter);
            actions.put(idCounter++, action);
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
        while (action.getNextRoutingAction() != null && action.isChainable() && counter < 10) {
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
                list.stream().map(a -> a.getId().toString()).collect(Collectors.joining(",")) );
        
        // The last Action will contain the summation of all the attrs
        list.getLast().setTransactionAttributes(attrs);
        return list;
    }

    private Action getCurrentAction(SMARequest event) throws CloneNotSupportedException {
        final var attrs = event.getCallDetails().getTransactionAttributes();

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
    public final SMAResponse handleRequest(SMARequest event, Context cntxt) {
        final boolean throwException = Boolean.parseBoolean(System.getenv("THROW_EXCEPTION"));
        if (throwException) {
            throw new RuntimeException("This region is down");
        }
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
                    if (action instanceof CallAndBridgeAction) {

                        final var attrs = action.getTransactionAttributes();
                        final var hangupB = event.getActionData().get("Type").equals("Hangup")
                                && ((Map<String, Object>) event.getActionData().get("Parameters")).get("ParticipantTag").equals("LEG-B");

                        if (hangupB) {
                            log.debug("Diconnect on leg B associated with Disconnect and Transfer");
                            ((CallAndBridgeAction) action).setUri((String) attrs.get("transferNumber"));
                            res = SMAResponse.builder()
                                    .withActions(List.of(action.getResponse()))
                                    .withTransactionAttributes(attrs)
                                    .build();
                            break;
                        }

                        // When a call is bridged successfully don't do anything
                        log.debug("CallAndBridge has connected call now, empty response");
                        res = SMAResponse.builder().build();
                    } else if (action instanceof StartBotConversationAction) {
                        // Always put last intent matched int the session
                        final var sbaction = (StartBotConversationAction) action;
                        log.debug("Lex Bot has finished and Intent is " + sbaction.getIntent());
                        action.getTransactionAttributes().put("LexLastMatchedIntent", sbaction.getIntent());
                        res = defaultResponse(action, event);
                    } else if (action instanceof StartCallRecordingAction) {
                        final var crd = (Map<String,Object>) event.getActionData().get("CallRecordingDestination");
                        final var loc = crd.get("Location");
                        log.debug("Start Call Recording SUCCESS with file " + loc);
                        action.getTransactionAttributes().put(StartCallRecordingAction.RECORDING_FILE_LOCATION, loc.toString() );
                        res = defaultResponse(action, event);
                    } else {
                        res = defaultResponse(action, event);
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
                case HANGUP:
                    final var disconnectedBy = event.getCallDetails().getTransactionAttributes().getOrDefault("Disconnect", "Application");
                    log.debug("Call Was disconnected by [" + disconnectedBy + "], sending empty response");
                    action = getCurrentAction(event);
                    if (action instanceof CallAndBridgeAction && event.getCallDetails().getParticipants().size() > 1) {
                        // We have two participants and one side has hung up (by caller or callee), so we should hang the other leg up as well
                        final var participant = event.getCallDetails().getParticipants().stream()
                                .filter(p -> p.getStatus().equals(Status.Connected))
                                .findAny().orElse(null);
                        if (participant != null) {
                            res = SMAResponse.builder().withActions(List.of(ResponseHangup.builder().withParameters(ResponseHangup.Parameters.builder().withParticipantTag(participant.getParticipantTag()).build()).build())).build();
                        } else {
                            res = SMAResponse.builder().build();
                        }
                    } else {
                        try {
                            hangupHandler(action);
                        } catch (Exception e) {
                            log.error("Exception in Hangup Handler", e);
                        }
                        res = SMAResponse.builder().build();
                    }
                    break;
                case CALL_UPDATE_REQUESTED:
                    action = getCurrentAction(event);
                    if (action instanceof CallAndBridgeAction) {
                        final var attrs = action.getTransactionAttributes();

                        final var params = event.getActionData().get("Parameters");
                        if (params != null && params instanceof Map) {
                            final var args = ((Map) params).get("Arguments");
                            if (args != null && args instanceof Map) {
                                final var phoneNumber = ((Map) args).get("phoneNumber");
                                if (phoneNumber != null) {
                                    log.debug("Update Requested with a transfer to number of " + phoneNumber);
                                    attrs.put("transferNumber", phoneNumber);
                                }
                            }
                        }
                        // Disconnect leg B
                        final var diconnectLegB = ResponseHangup.builder().withParameters(ResponseHangup.Parameters.builder().withParticipantTag(ParticipantTag.LEG_B).build()).build();
                        res = SMAResponse.builder()
                                .withActions(List.of(diconnectLegB))
                                .withTransactionAttributes(attrs)
                                .build();
                        break;
                    }

                default:
                    log.debug("Invocation type is unhandled, sending empty response " + event.getInvocationEventType());
                    res = SMAResponse.builder().build();
            }

            //log.debug(res);
            log.debug(mapper.valueToTree(res).toString());
            return res;
        } catch (Exception e) {
            log.error("Unhandled Exception", e);
            return SMAResponse.builder().withActions(List.of(ResponseHangup.builder().build())).build();
        }
    }

    private SMAResponse defaultResponse(Action action, SMARequest event) throws CloneNotSupportedException {
        SMAResponse res;
        if (event.getInvocationEventType().equals(ACTION_SUCCESSFUL) && Disconnected.equals(event.getCallDetails().getParticipants().get(0).getStatus()) ) {
            // We are just getting a success on the last Action while caller hung up, so we can't go to next action
            log.debug("Call is Disconnected on ACTION_SUCCESSFUL, so empty response");
            res = SMAResponse.builder().build();
        } else if (action.getNextRoutingAction() != null) {
            final var actionList = getActions(action.getNextRoutingAction(), event);
            res = SMAResponse.builder().withTransactionAttributes(actionList.getLast().getTransactionAttributes())
                    .withActions(actionList.stream().map(a -> a.getResponse()).collect(Collectors.toList())).build();
            log.info("Moving to next action: " + actionList.getFirst().getDebugSummary());
        } else {
            // If no action next, then end with hang up
            log.info("No next action in flow, ending call with hangup");
            res = SMAResponse.builder().withActions(List.of(ResponseHangup.builder().build())).build();
        }
        return res;
    }

    /**
     * Hangup LEG-A of the call (inbound call)
     *
     * @return
     */
    private SMAResponse hangupLegA() {
        return SMAResponse.builder().withActions(List.of(ResponseHangup.builder().build())).build();
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
