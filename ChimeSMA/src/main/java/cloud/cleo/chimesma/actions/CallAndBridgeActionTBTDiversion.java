/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.ResponseAction;
import cloud.cleo.chimesma.model.ResponseCallAndBridge;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.extensions.AutoGeneratedTimestampRecordExtension;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbAutoGeneratedTimestampAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * Special Call And Bridge that generates a key and stores SMA call info in Dynamo Table
 * When the call is transferred the receiving instance (like Amazon Conenect) can then
 * inspect the Diversion header and use it to retrieve call info and call Chime API to update the call 
 * @author sjensen
 */
@Data
public class CallAndBridgeActionTBTDiversion extends CallAndBridgeAction {

    final static TableSchema<SMACall> schema = TableSchema.fromBean(SMACall.class);

    final static DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
            .extensions(AutoGeneratedTimestampRecordExtension.create()).build();

    final static DynamoDbTable<SMACall> calls = enhancedClient.table(System.getenv("CALLS_TABLE_NAME"), schema);

    public CallAndBridgeActionTBTDiversion(Integer callTimeoutSeconds, String callerIdNumber, Map<String, String> sipHeaders, ResponseCallAndBridge.BridgeEndpointType bridgeEndpointType, String arn, String uri, String bucketName, String key, String keyLocale) {
        super(callTimeoutSeconds, callerIdNumber, sipHeaders, bridgeEndpointType, arn, uri, bucketName, key, keyLocale);
    }

    
    @Override
    public ResponseAction getResponse() {
        final var phoneNumKey = generateRandomPhoneNumber();
        final var cd = getEvent().getCallDetails();

        // Write a Dynamo record to a global call table
        final var call = new SMACall();
        call.setPhoneNumber(phoneNumKey);
        call.setSipMediaApplicationId(cd.getSipMediaApplicationId());
        call.setTransactionId(cd.getTransactionId());
        call.setRegion(cd.getAwsRegion());
        call.setTtl(Instant.now().plus(Duration.ofDays(1)).getEpochSecond());
        calls.putItem(call);

        if (sipHeaders == null) {
            sipHeaders = new HashMap<>();
        }

        // Place Info about this SMA into the Diversion Header
        sipHeaders.put("Diversion", "sip:" + phoneNumKey + "@0.0.0.0");

        return super.getResponse();
    }
    
    public static CallAndBridgeActionTBTDiversionBuilder builder() {
        return new CallAndBridgeActionTBTDiversionBuilder();
    }
    
    public static class CallAndBridgeActionTBTDiversionBuilder extends CallAndBridgeActionBuilder {
        
        @Override
        protected CallAndBridgeActionTBTDiversion buildImpl() {
            return new CallAndBridgeActionTBTDiversion(callTimeoutSeconds, callerIdNumber, sipHeaders, bridgeEndpointType, arn, uri, bucketName, key, keyLocale);
        }
    }

    @DynamoDbBean
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class SMACall {

        private String phoneNumber;
        private String sipMediaApplicationId;
        private String transactionId;
        private String region;
        private Instant lastUpdate;
        private Long ttl;

        @DynamoDbPartitionKey
        public String getPhoneNumber() {
            return phoneNumber;
        }

        @DynamoDbAutoGeneratedTimestampAttribute
        public Instant getLastUpdate() {
            return lastUpdate;
        }
    }

    private static String generateRandomPhoneNumber() {
        final var rand = new Random();
        DecimalFormat df = new DecimalFormat("+16122000000");
        // This generates a random number between 0 (inclusive) and 1000000000 (exclusive), ensuring 9 digits.
        int randomNumber = rand.nextInt(1000000);
        return df.format(randomNumber);
    }

}
