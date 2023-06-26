/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Model for a Chime SDK Sip Media Application Telephony Event
 * https://docs.aws.amazon.com/chime-sdk/latest/dg/pstn-invocations.html
 *
 * @author sjensen
 */
@Data
@Builder(setterPrefix = "with")
@NoArgsConstructor
@AllArgsConstructor
public class SMARequest implements Serializable {

    @JsonProperty("SchemaVersion")
    private String schemaVersion;
    @JsonProperty("Sequence")
    private Integer sequence;
    @JsonProperty("InvocationEventType")
    private SMAEventType invocationEventType;
    @JsonProperty("CallDetails")
    private CallDetails callDetails;

    // Set on INVALID_LAMBDA_RESPONSE
    @JsonProperty("ErrorType")
    private String errorType;
    @JsonProperty("ErrorMessage")
    private String errorMessage;

    @JsonProperty("ActionData")
    private Map<String, Object> actionData;

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallDetails implements Serializable {

        @JsonProperty("TransactionId")
        private String transactionId;
        @JsonProperty("TransactionAttributes")
        private Map<String, Object> transactionAttributes;

        @JsonProperty("AwsAccountId")
        private String awsAccountId;
        @JsonProperty("AwsRegion")
        private String awsRegion;
        @JsonProperty("SipMediaApplicationId")
        private String sipMediaApplicationId;
        @JsonProperty("Participants")
        private List<Participant> participants;
    }

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Participant implements Serializable {

        @JsonProperty("CallId")
        private String callId;
        @JsonProperty("ParticipantTag")
        private ParticipantTag participantTag;
        @JsonProperty("To")
        private String to;
        @JsonProperty("From")
        private String from;
        @JsonProperty("Direction")
        private Direction direction;
        @JsonProperty("StartTimeInMilliseconds")
        @JsonDeserialize(converter = InstantConverter.class)
        private Instant startTime;
        @JsonProperty("Status")
        private Status status;
    }

    public enum SMAEventType {
        NEW_INBOUND_CALL,
        NEW_OUTBOUND_CALL,
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        ACTION_INTERRUPTED,
        HANGUP,
        CALL_ANSWERED,
        INVALID_LAMBDA_RESPONSE,
        DIGITS_RECEIVED,
        CALL_UPDATE_REQUESTED,
        RINGING
    }

    public enum Direction {
        Inbound,
        Outbound
    }

    public enum Status {
        Connected,
        Disconnected
    }

    private static class InstantConverter extends StdConverter<String, Instant> {

        @Override
        public Instant convert(String in) {
            return Instant.ofEpochMilli(Long.decode(in));
        }

    }
}
