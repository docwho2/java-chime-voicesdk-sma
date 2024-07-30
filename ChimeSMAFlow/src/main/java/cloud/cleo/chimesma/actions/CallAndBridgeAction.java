package cloud.cleo.chimesma.actions;

import static cloud.cleo.chimesma.actions.Action.log;
import static cloud.cleo.chimesma.model.ParticipantTag.*;
import cloud.cleo.chimesma.model.ResponseAction;
import cloud.cleo.chimesma.model.ResponseActionType;
import static cloud.cleo.chimesma.model.ResponseActionType.CallAndBridge;
import cloud.cleo.chimesma.model.ResponseCallAndBridge;
import cloud.cleo.chimesma.model.ResponsePlayAudio;
import static cloud.cleo.chimesma.model.SMARequest.SMAEventType.ACTION_SUCCESSFUL;
import cloud.cleo.chimesma.model.SMARequest.Status;
import static cloud.cleo.chimesma.model.SMARequest.Status.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 *
 * @author sjensen
 */
@Data
@SuperBuilder(setterPrefix = "with")
public class CallAndBridgeAction extends Action<CallAndBridgeAction, ResponseCallAndBridge> {

    /**
     * Description – The interval before a call times out. The timer starts at call setup.
     *
     * Allowed values – Between 1 and 120, inclusive Required – No Default value – 30
     *
     */
    protected Integer callTimeoutSeconds;

    protected String callerIdNumber;
    protected Function<CallAndBridgeAction, String> callerIdNumberF;

    protected final Map<String, String> sipHeaders = new HashMap<>();

    protected String arn;
    protected Function<CallAndBridgeAction, String> arnF;

    protected String uri;
    protected Function<CallAndBridgeAction, String> uriF;

    // RingbackTone
    @Builder.Default
    protected String ringbackToneBucketName = System.getenv("PROMPT_BUCKET");
    /**
     * The Key !
     */
    protected String ringbackToneKey;
    protected String ringbackToneKeyLocale;

    /**
     * Action to take when there is a hang up on LegB (outgoing leg). Use case, App dials out to someone, then they hang
     * or its disconnected on purpose, but then you want to return the caller (leg A) back into IVR app and not hang up
     * on them.
     */
    protected Action nextLegBHangupAction;
    protected Function<CallAndBridgeAction, Action> nextLegBHangupActionF;

    @Override
    protected ResponseAction getResponse() {

        ResponsePlayAudio.AudioSource audioSource = null;
        if ((getRingbackToneKey() != null || getRingbackToneKeyLocale() != null) && getRingbackToneBucketName() != null) {

            final String myKey;
            if (getRingbackToneKeyLocale() != null) {
                myKey = getRingbackToneKeyLocale() + "-" + getLocale().toLanguageTag() + ".wav";
            } else {
                myKey = getRingbackToneKey();
            }

            audioSource = ResponsePlayAudio.AudioSource.builder()
                    .withBucketName(getRingbackToneBucketName())
                    .withKey(myKey)
                    .build();
        }

        final var myArn = getFuncValOrDefault(arnF, arn);
        final var myUri = getFuncValOrDefault(uriF, uri);

        final var endpoint = ResponseCallAndBridge.Endpoint.builder()
                .withArn(myArn)
                // When Arn is set, then must be a AWS (SIP route), when this is null, we route to PSTN
                .withBridgeEndpointType(myArn != null ? ResponseCallAndBridge.BridgeEndpointType.AWS : ResponseCallAndBridge.BridgeEndpointType.PSTN)
                .withUri(myUri)
                .build();

        if (getFuncValOrDefault(callerIdNumberF, callerIdNumber) == null) {
            // Set to from Number if not provided because this is a required param
            setCallerIdNumber(getEvent().getCallDetails().getParticipants().get(0).getFrom());
        }

        final var params = ResponseCallAndBridge.Parameters.builder()
                .withCallTimeoutSeconds(getCallTimeoutSeconds())
                .withCallerIdNumber(getFuncValOrDefault(callerIdNumberF, callerIdNumber))
                .withRingbackTone(audioSource)
                .withEndpoints(List.of(endpoint))
                .withSipHeaders(getSipHeaders().isEmpty() ? null : getSipHeaders())
                .build();
        return ResponseCallAndBridge.builder().withParameters(params).build();
    }

    @Override
    protected Action getNextRoutingAction() {
        if (getEvent() != null) {
            switch (getEvent().getInvocationEventType()) {
                case ACTION_SUCCESSFUL -> {
                    final var ad = getEvent().getActionData();
                    switch (ad.getType()) {
                        case CallAndBridge -> {
                            // When a call is bridged successfully don't do anything
                            log.debug("CallAndBridge has connected call now, empty response");
                            return null;
                        }
                        default -> {
                            return super.getNextRoutingAction();
                        }
                    }
                }
                default -> {
                    return super.getNextRoutingAction();
                }
            }
        }
        return super.getNextRoutingAction();
    }

    /**
     * Hangup events are dispatched to CallAndBridge Actions.
     *
     * @return
     */
    protected Action getHangupAction() {
        if (getEvent() != null) {
            log.debug("CallAndBridge Hangup Event processing");
            final var partL = getEvent().getCallDetails().getParticipants();

            if (nextLegBHangupActionF != null || nextLegBHangupAction != null) {
                log.debug("Hangup Action on Leg-B disconnect set, checking for scenario of A=Connected and B=Disconnected");
                if (partL.size() == 2) {
                    final var legACon = partL.stream().anyMatch(p -> p.getParticipantTag().equals(LEG_A) && p.getStatus().equals(Connected));
                    final var legBDis = partL.stream().anyMatch(p -> p.getParticipantTag().equals(LEG_B) && p.getStatus().equals(Disconnected));
                    if (legACon && legBDis) {
                        log.debug("LegB has disconnected and LegA is still connected, performing LegBHangupAction");
                        return getFuncValOrDefault(nextLegBHangupActionF, nextLegBHangupAction);
                    }
                }
            }

            // Normal Disconnect Scenario, where any leg that is hung up, we must hang the other leg up or call will sit forever
            if (partL.size() > 1) {
                // We have two participants and one side has hung up (by caller or callee), so we should hang the other leg up as well
                final var participant = partL.stream()
                        .filter(p -> p.getStatus().equals(Status.Connected))
                        .findAny().orElse(null);
                // Disconnet the participant that is still in a connected state
                if (participant != null) {
                    log.debug("CallAndBridge Hangup Event, 2 participants, One still connected, disconnecting " + participant.getParticipantTag());
                    return HangupAction.builder().withParticipantTag(participant.getParticipantTag()).build();
                }
            }
        }
        return null;
    }

    @Override
    protected StringBuilder getDebugSummary() {
        return super.getDebugSummary()
                .append(" [").append(getUri()).append(']');
    }

    @Override
    protected void onActionSuccessful() {
    }

    @Override
    protected boolean isChainable() {
        return false;
    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.CallAndBridge;
    }

}
