/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.model;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.util.StdConverter;
import java.io.IOException;

/**
 * Since regular enums can't have dashes in the name, we need to 
 * have custom serialize and de-serialization
 * 
 * @author sjensen
 */
@JsonSerialize(using = ParticipantTag.ParticipantTagSerializer.class)
@JsonDeserialize(converter = ParticipantTag.TagConverter.class)
public enum ParticipantTag {
    LEG_A("LEG-A"),
    LEG_B("LEG-B");

    private final String value;

    private ParticipantTag(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    static class ParticipantTagSerializer extends StdSerializer<ParticipantTag> {

        public ParticipantTagSerializer() {
            super(ParticipantTag.class);
        }

        @Override
        public void serialize(ParticipantTag tag, JsonGenerator jg, SerializerProvider sp) throws IOException {
            jg.writeString(tag.toString());
        }

    }
    
    static class TagConverter extends StdConverter<String, ParticipantTag>  {
        @Override
        public ParticipantTag convert(String in) {
           return ParticipantTag.valueOf(in.replace("-", "_"));
        }
        
    }

}
