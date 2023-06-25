/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.ResponseAction;
import cloud.cleo.chimesma.model.ResponseCallAndBridge;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

/**
 * Special Call and Bridge Action that also populates SIP headers with SMA call details 
 * 
 * @author sjensen
 */
@Data
public class CallAndBridgeActionTBTSIP extends CallAndBridgeAction {

    public CallAndBridgeActionTBTSIP(Integer callTimeoutSeconds, String callerIdNumber, Map<String, String> sipHeaders, ResponseCallAndBridge.BridgeEndpointType bridgeEndpointType, String arn, String uri, String bucketName, String key, String keyLocale) {
        super(callTimeoutSeconds, callerIdNumber, sipHeaders, bridgeEndpointType, arn, uri, bucketName, key, keyLocale);
    }

    
    @Override
    public ResponseAction getResponse() {
        final var cd = getEvent().getCallDetails();

        if (sipHeaders == null) {
            sipHeaders = new HashMap<>();
        }

         // Place Info about this SMA into the SIP headers to allow call backs to Chime API for this call
        sipHeaders.put("x-sma-AwsRegion", cd.getAwsRegion());
        sipHeaders.put("x-sma-SipMediaApplicationId", cd.getSipMediaApplicationId());
        sipHeaders.put("x-sma-TransactionId", cd.getTransactionId());

        return super.getResponse();
    }
    
    public static CallAndBridgeActionTBTSIPBuilder builder() {
        return new CallAndBridgeActionTBTSIPBuilder();
    }
    
    public static class CallAndBridgeActionTBTSIPBuilder extends CallAndBridgeActionBuilder {
        
        @Override
        protected CallAndBridgeActionTBTSIP buildImpl() {
            return new CallAndBridgeActionTBTSIP(callTimeoutSeconds, callerIdNumber, sipHeaders, bridgeEndpointType, arn, uri, bucketName, key, keyLocale);
        }
    }


}
