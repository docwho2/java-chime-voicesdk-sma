/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma.flows;

import cloud.cleo.chimesma.actions.*;
import cloud.cleo.chimesma.model.ResponseSpeak.VoiceId;
import cloud.cleo.chimesma.model.ResponseStartCallRecording;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author sjensen
 */
public class ExampleFlow extends AbstractFlow {

    @Override
    protected Action getInitialAction() {

        final var hangup = HangupAction.builder()
                .withDescription("This is my last step").build();

        final var goodbye = PlayAudioAction.builder()
                .withDescription("END FLOW")
                .withKeyLocale("goodbye")
                .withNextAction(hangup)
                .build();

        final var speak3 = SpeakAction.builder()
                .withText("This is the last part")
                .withVoiceId(VoiceId.Emma)
                //.withNextAction(hangup)
                .build();

        final var speak2 = SpeakAction.builder().
                withText("This is part 2")
                .withNextAction(speak3).build();

        final var speak = SpeakAction.builder()
                .withText("Welcome to the new age")
                .withVoiceId(VoiceId.Salli)
                .withNextAction(speak2).build();

        final var start = PlayAudioAction.builder()
                .withKey("main.wav")
                .withNextAction(speak).build();

        final var hiddenMenu = SpeakAction.builder()
                .withText("You have reached the special Hidden menu!")
                .withVoiceId(VoiceId.Salli)
                .withNextAction(start)
                .build();

        final var digits = ReceiveDigitsAction.builder()
                .withInBetweenDigitsDurationInMilliseconds(700)
                .withFlushDigitsDurationInMilliseconds(2000)
                .withInputDigitsRegex("^\\*\\*$")
                .withNextAction(start)
                .withDigitsRecevedAction(hiddenMenu).build();

        final var connect = CallAndBridgeActionTBTDiversion.builder()
                .withUri("+15052162949")
                .withRingbackToneKey("transfer.wav")
                .build();

        final var call = CallAndBridgeAction.builder()
                .withUri("+16122260725")
                .withRingbackToneKey("transfer.wav")
                .build();

        final var weatherBot = StartBotConversationAction.builder()
                .withDescription("Weather Bot")
                .withBotAliasArn(System.getenv("WEATHER_BOT_ALIAS_ARN"))
                .withContent("You are now at the weather lex bot")
                .build();

        final var chatGptBot = StartBotConversationAction.builder()
                .withDescription("ChatGPT Bot")
                .withBotAliasArn(System.getenv("CHATGPT_BOT_ALIAS_ARN"))
                .withContent("Go ahead ask Chat GPT anything")
                .withLocale(Locale.forLanguageTag("en-US"))
                .build();

        final var lexBot = StartBotConversationAction.builder()
        .withDescription("Main Bot")
                .withContent("Press One for Weather, Two for for Representative, or simply tell me how I can help?")
                .withNextAction(speak3)
                .withNextAction(a -> {
                    switch (a.getIntent()) {
                        case "Weather":
                            return weatherBot;
                        case "Transfer":
                            return connect;
                        case "Quit":
                            return goodbye;
                        default:
                            return chatGptBot;
                    }
                })
                .build();

        chatGptBot.setNextActionFunction(a -> {
            switch (a.getIntent()) {
                case "Quit":
                    return goodbye;
                default:
                    return lexBot;
            }
        });

        weatherBot.setNextActionFunction((a) -> {
            switch (a.getIntent()) {
                case "Quit":
                    return goodbye;
                default:
                    return lexBot;
            }
        });
        
        
        final var resumeRecording = ResumeCallRecordingAction.builder()
                .withNextAction(lexBot)
                .build();
       

        final var speakGetDigits = SpeakAndGetDigitsAction.builder()
                .withSpeechParameters(SpeakAndGetDigitsAction.SpeechParameters.builder()
                        .withText("Plese enter some digits").build())
                .withFailureSpeechParameters(SpeakAndGetDigitsAction.SpeechParameters.builder()
                        .withText("Plese try again").build())
                .withRepeatDurationInMilliseconds(3000)
                .withRepeat(2)
                .withTerminatorDigits(List.of('#','9'))
                .withInputDigitsRegex("^\\d{2}$")
                .withNextAction(resumeRecording)
                .build();
        
         final var pauseRecording = PauseCallRecordingAction.builder()
                .withNextAction(speakGetDigits)
                .build();
 
        
        final var region = SpeakAction.builder()
                .withText(f -> "The region is " + System.getenv("AWS_REGION").replace("-", " ").toUpperCase())
                .withVoiceId(VoiceId.Salli)
                .withNextAction(getPauseActions(pauseRecording))
                .build();
        
         final var startRecording = StartCallRecordingAction.builder()
                .withTrack(ResponseStartCallRecording.Track.BOTH)
                .withNextAction(region)
                .build();

        return startRecording;
    }
    
    
    public Action getPauseActions(Action nextAction) {
        // Build a set of Pause Actions to test library sending no more than 10 Actions
        final int eventCount = 11;
        Action startLoop = null;
        Action lastLoop = null;
        for(int i = 1; i <= eventCount; i++ ) {
            Action a = PauseAction.builder()
                    .withDescription("Pause " + i)
                    .withDurationInMilliseconds(100+i)
                    .build();
            
            switch (i) {
                case 1:
                    startLoop = a;
                    break;
                case eventCount:
                    lastLoop.setNextAction(a);
                    a.setNextAction(nextAction);
                    break;
                default:
                    lastLoop.setNextAction(a);
                    break;
            }
            lastLoop = a;
        }
        return startLoop;
    }

    @Override
    protected Action getErrorAction() {
        final var errMsg = SpeakAction.builder()
                .withText("A system error has occured, please call back and try again")
                .build();

        return errMsg;
    }

    @Override
    protected void newCallHandler(Action action) {
        log.info("New Call Handler Code Here");
    }

    @Override
    protected void hangupHandler(Action action) {
        log.info("Hangup Handler Code Here");
    }

}
