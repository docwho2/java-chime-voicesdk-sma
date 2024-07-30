package cloud.cleo.chimesma.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * SMA Application Lambda Response.
 * <p>
 * By default, SIP media applications time out if a Lambda function doesn't respond after 20 seconds.
 *
 * @author sjensen
 * @see <a href="https://docs.aws.amazon.com/chime-sdk/latest/dg/invoke-on-call-leg.html">AWS Documentation</a>
 */
@Data
@Builder(setterPrefix = "with")
@NoArgsConstructor
@AllArgsConstructor
public class SMAResponse implements Serializable {

    @JsonProperty("SchemaVersion")
    private final String schemaVersion = "1.0";

    /**
     * You can respond to an AWS Lambda invocation event with a list of actions to run on the individual participants in
     * a call. You can respond with a maximum of 10 actions for each AWS Lambda invocation, and you can invoke an AWS
     * Lambda function 1,000 times per call.
     */
    @JsonProperty("Actions")
    @Builder.Default
    private List<ResponseAction> actions = List.of();

    /**
     * You use the TransactionAttributes data structure to store application-specific information, such as call states
     * or meeting IDs, and then pass that data to AWS Lambda invocations. This structure removes the need for storing
     * data in external databases such as Amazon DynamoDB.
     *
     * TransactionAttributes are JSON Objects that contain key/value pairs. The objects can contain a maximum of 100
     * key/value pairs, and the objects have a maximum payload size of 20 KB. The data in a TransactionAttributes
     * structure persists for the life of a transaction.
     *
     * When an AWS Lambda function passes TransactionAttributes to a SIP media application, the application updates any
     * stored attributes. If you pass a TransactionAttributes object with an existing key set, you update the stored
     * values. If you pass a different key set, you replace the existing values with the values from that different key
     * set. Passing an empty map ( {} ) erases any stored values.
     *
     * @see <a href="https://docs.aws.amazon.com/chime-sdk/latest/dg/transaction-attributes.html">AWS Documentation</a>
     */
    @JsonProperty("TransactionAttributes")
    @JsonInclude(Include.NON_NULL)
    private Map<String, Object> transactionAttributes;

}
