/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.polly.model.Engine;
import software.amazon.awssdk.services.polly.model.LanguageCode;
import software.amazon.awssdk.services.polly.model.TextType;
import software.amazon.awssdk.services.polly.model.VoiceId;

/**
 *
 * @author sjensen
 */
@Data
@Builder(setterPrefix = "with")
@NoArgsConstructor
@AllArgsConstructor
public class ResponseSpeak implements ResponseAction, Serializable {

    private final ResponseActionType type = ResponseActionType.Speak;
    @JsonProperty(value = "Parameters")
    private Parameters parameters;

    @Data
    @Builder(setterPrefix = "with")
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    public static class Parameters implements Serializable {

        @JsonProperty(value = "Text")
        private String text;
        @JsonProperty(value = "CallId")
        private String callId;
        @JsonProperty(value = "Engine")
        @JsonSerialize(using = EngineSerializer.class)
        private Engine engine;
        @JsonProperty(value = "LanguageCode")
        @JsonSerialize(using = LanguageCodeSerializer.class)
        private LanguageCode languageCode;
        @JsonProperty(value = "TextType")
        @JsonSerialize(using = TextTypeSerializer.class)
        private TextType textType;
        @JsonProperty(value = "VoiceId")
        @JsonSerialize(using = VoiceIdSerializer.class)
        private VoiceId voiceId;
    }

    static class EngineSerializer extends StdSerializer<Engine> {

        public EngineSerializer() {
            super(Engine.class);
        }

        @Override
        public void serialize(Engine val, JsonGenerator jg, SerializerProvider sp) throws IOException {
            jg.writeString(val.toString());
        }
    }

    static class LanguageCodeSerializer extends StdSerializer<LanguageCode> {

        public LanguageCodeSerializer() {
            super(LanguageCode.class);
        }

        @Override
        public void serialize(LanguageCode val, JsonGenerator jg, SerializerProvider sp) throws IOException {
            jg.writeString(val.toString());
        }
    }

    static class TextTypeSerializer extends StdSerializer<TextType> {

        public TextTypeSerializer() {
            super(TextType.class);
        }

        @Override
        public void serialize(TextType val, JsonGenerator jg, SerializerProvider sp) throws IOException {
            jg.writeString(val.toString());
        }
    }

    static class VoiceIdSerializer extends StdSerializer<VoiceId> {

        public VoiceIdSerializer() {
            super(VoiceId.class);
        }

        @Override
        public void serialize(VoiceId voiceId, JsonGenerator jg, SerializerProvider sp) throws IOException {
            jg.writeString(voiceId.toString());
        }
    }

}
