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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author sjensen
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallAndBridgeAction extends Action<CallAndBridgeAction> {

    protected Integer callTimeoutSeconds;
    protected String callerIdNumber;

    protected Map<String, String> sipHeaders;

    protected ResponseCallAndBridge.BridgeEndpointType bridgeEndpointType = ResponseCallAndBridge.BridgeEndpointType.PSTN;
    protected String arn;
    protected String uri;

    // RingbackTone
    protected String bucketName = System.getenv("PROMPT_BUCKET");
    protected String key;
    protected String keyLocale;

    @Override
    public ResponseAction getResponse() {

        ResponsePlayAudio.Parameters.AudioSource audioSource = null;
        if ((key != null || keyLocale != null) && bucketName != null) {

            final String myKey;
            if (keyLocale != null) {
                myKey = keyLocale + "-" + getLocale().toLanguageTag() + ".wav";
            } else {
                myKey = key;
            }

            audioSource = ResponsePlayAudio.Parameters.AudioSource.builder()
                    .withBucketName(bucketName)
                    .withKey(myKey)
                    .build();
        }

        final var endpoint = ResponseCallAndBridge.Parameters.Endpoint.builder()
                .withArn(arn)
                .withBridgeEndpointType(bridgeEndpointType)
                .withUri(uri)
                .build();

        if (callerIdNumber == null) {
            // Set to from Number if not provided
            callerIdNumber = getEvent().getCallDetails().getParticipants().get(0).getFrom();
        }

        final var params = ResponseCallAndBridge.Parameters.builder()
                .withCallTimeoutSeconds(callTimeoutSeconds)
                .withCallerIdNumber(callerIdNumber)
                .withRingbackTone(audioSource)
                .withEndpoints(List.of(endpoint))
                .withSipHeaders(sipHeaders)
                .build();
        return ResponseCallAndBridge.builder().withParameters(params).build();
    }

    @Override
    protected StringBuilder getDebugSummary() {
        return super.getDebugSummary()
                .append(" [").append(getUri()).append(']');
    }

    public static CallAndBridgeActionBuilder builder() {
        return new CallAndBridgeActionBuilder();
    }

    @NoArgsConstructor
    public static class CallAndBridgeActionBuilder extends ActionBuilder<CallAndBridgeActionBuilder, CallAndBridgeAction> {

        protected Integer callTimeoutSeconds;
        protected String callerIdNumber;
        protected Map<String, String> sipHeaders;

        protected ResponseCallAndBridge.BridgeEndpointType bridgeEndpointType = ResponseCallAndBridge.BridgeEndpointType.PSTN;
        protected String arn;
        protected String uri;

        protected String bucketName = System.getenv("PROMPT_BUCKET");
        protected String key;
        protected String keyLocale;

        public CallAndBridgeActionBuilder withCallTimeoutSeconds(Integer value) {
            this.callTimeoutSeconds = value;
            return this;
        }

        public CallAndBridgeActionBuilder withCallerIdNumber(String value) {
            this.callerIdNumber = value;
            return this;
        }

        public CallAndBridgeActionBuilder withSipHeaders(Map<String, String> value) {
            this.sipHeaders = value;
            return this;
        }

        public CallAndBridgeActionBuilder withBridgeEndpointType(ResponseCallAndBridge.BridgeEndpointType value) {
            this.bridgeEndpointType = value;
            return this;
        }

        public CallAndBridgeActionBuilder withArn(String value) {
            this.arn = value;
            return this;
        }

        public CallAndBridgeActionBuilder withUri(String value) {
            this.uri = value;
            return this;
        }

        public CallAndBridgeActionBuilder withRingbackToneBucketName(String value) {
            this.bucketName = value;
            return this;
        }

        public CallAndBridgeActionBuilder withRingbackToneKey(String value) {
            this.key = value;
            return this;
        }

        public CallAndBridgeActionBuilder withRingbackToneKeyLocale(String value) {
            this.keyLocale = value;
            return this;
        }

        @Override
        protected CallAndBridgeAction buildImpl() {
            return new CallAndBridgeAction(callTimeoutSeconds, callerIdNumber, sipHeaders, bridgeEndpointType, arn, uri, bucketName, key, keyLocale);
        }
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
