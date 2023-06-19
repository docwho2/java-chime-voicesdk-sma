/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
 *
 * @author sjensen
 */
@Data
@Builder(setterPrefix = "with")
@NoArgsConstructor
@AllArgsConstructor
public class SMAResponse implements Serializable {

    @JsonProperty("SchemaVersion")
    private final String schemaVersion = "1.0";
    @JsonProperty("Actions")
    @Builder.Default
    private List<ResponseAction> actions = List.of();
    @JsonProperty("TransactionAttributes")
    @JsonInclude(Include.NON_NULL)
    private Map<String, Object> transactionAttributes;
    
}
