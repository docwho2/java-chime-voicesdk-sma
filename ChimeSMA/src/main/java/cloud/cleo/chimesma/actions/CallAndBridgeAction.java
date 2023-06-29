/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.ResponseAction;
import cloud.cleo.chimesma.model.ResponseActionType;
import cloud.cleo.chimesma.model.ResponseCallAndBridge;
import cloud.cleo.chimesma.model.ResponsePlayAudio;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
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
     * Allowed values – Between 1 and 120, inclusive
     * Required – No
     * Default value – 30
     *
     */
    protected Integer callTimeoutSeconds;
    protected String callerIdNumber;

    protected Map<String, String> sipHeaders;

    @Builder.Default
    protected ResponseCallAndBridge.BridgeEndpointType bridgeEndpointType = ResponseCallAndBridge.BridgeEndpointType.PSTN;
    protected String arn;
    protected String uri;

    // RingbackTone
    @Builder.Default
    protected String ringbackToneBucketName = System.getenv("PROMPT_BUCKET");
    /**
     * The Key !
     */
    protected String ringbackToneKey;
    protected String ringbackToneKeyLocale;
    
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

        final var endpoint = ResponseCallAndBridge.Endpoint.builder()
                .withArn(getArn())
                .withBridgeEndpointType(getBridgeEndpointType())
                .withUri(getUri())
                .build();

        if (getCallerIdNumber() == null) {
            // Set to from Number if not provided
            setCallerIdNumber(getEvent().getCallDetails().getParticipants().get(0).getFrom());
        }

        final var params = ResponseCallAndBridge.Parameters.builder()
                .withCallTimeoutSeconds(getCallTimeoutSeconds())
                .withCallerIdNumber(getCallerIdNumber())
                .withRingbackTone(audioSource)
                .withEndpoints(List.of(endpoint))
                .withSipHeaders(getSipHeaders())
                .build();
        return ResponseCallAndBridge.builder().withParameters(params).build();
    }

    @Override
    protected StringBuilder getDebugSummary() {
        return super.getDebugSummary()
                .append(" [").append(getUri()).append(']');
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
