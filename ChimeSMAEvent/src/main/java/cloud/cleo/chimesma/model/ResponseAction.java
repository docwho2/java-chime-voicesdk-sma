/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package cloud.cleo.chimesma.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author sjensen
 */
public interface ResponseAction {

    @JsonProperty(value = "Type")
    public ResponseActionType getType();

}
