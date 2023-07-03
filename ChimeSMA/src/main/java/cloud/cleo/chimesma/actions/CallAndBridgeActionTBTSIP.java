/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import cloud.cleo.chimesma.model.ResponseAction;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * Special Call and Bridge Action that also populates SIP headers with SMA call details 
 * 
 * @author sjensen
 */
@Data
@SuperBuilder(setterPrefix = "with")
public class CallAndBridgeActionTBTSIP extends CallAndBridgeAction {

    
    @Override
    protected ResponseAction getResponse() {
        final var cd = getEvent().getCallDetails();

        final var sh = getSipHeaders();
       
         // Place Info about this SMA into the SIP headers to allow call backs to Chime API for this call
        sh.put("x-sma-AwsRegion", cd.getAwsRegion());
        sh.put("x-sma-SipMediaApplicationId", cd.getSipMediaApplicationId());
        sh.put("x-sma-TransactionId", cd.getTransactionId());

        return super.getResponse();
    }

}
