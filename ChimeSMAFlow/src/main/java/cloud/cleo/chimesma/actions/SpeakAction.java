/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.actions;

import static cloud.cleo.chimesma.actions.AbstractFlow.voice_map;
import cloud.cleo.chimesma.model.ResponseAction;
import cloud.cleo.chimesma.model.ResponseActionType;
import cloud.cleo.chimesma.model.ResponseSpeak;
import cloud.cleo.chimesma.model.ResponseSpeak.Engine;
import cloud.cleo.chimesma.model.ResponseSpeak.VoiceId;
import java.util.function.Function;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 *
 * @author sjensen
 */
@Data
@SuperBuilder(setterPrefix = "with")
public class SpeakAction extends Action<SpeakAction,ResponseSpeak> {

    /**
     * The text you want to Speak!
     */
    protected String text;
    /**
     * The function you want to generate the text
     */
    protected Function<SpeakAction, String> textF;

    protected Engine engine;
    protected VoiceId voiceId;

    @Override
    protected ResponseAction getResponse() {
        var myContent = getFuncValOrDefault(textF,text);
        final var params = ResponseSpeak.Parameters.builder()
                .withCallId(getCallId())
                .withEngine(engine != null ? engine : Engine.neural)
                .withLanguageCode(getLocale().toLanguageTag())
                .withText(myContent)
                .withTextType(getSpeakContentType(myContent))
                .withVoiceId(voiceId != null ? voiceId : voice_map.get(getLocale()))
                .build();
        return ResponseSpeak.builder().withParameters(params).build();
    }

    @Override
    public ResponseActionType getActionType() {
        return ResponseActionType.Speak;
    }

    @Override
    protected StringBuilder getDebugSummary() {
        final var sb = super.getDebugSummary();

        if (getFuncValOrDefault(textF,text) != null) {
            sb.append(" text=[").append(getFuncValOrDefault(textF,text)).append(']');
        }

        if (getEngine() != null) {
            sb.append(" engine=[").append(getEngine()).append(']');
        }

        if (getVoiceId() != null) {
            sb.append(" vid=[").append(getVoiceId()).append(']');
        }

        return sb;
    }
    
    /**
     * Given message content, determine if the message is SSML or just plain text
     *
     * @param message
     * @return
     */
    private static ResponseSpeak.TextType getSpeakContentType(String message) {
        if (message != null) {
            return message.toLowerCase().contains("<speak>") ? ResponseSpeak.TextType.ssml : ResponseSpeak.TextType.text;
        }
        return ResponseSpeak.TextType.text;
    }

}
