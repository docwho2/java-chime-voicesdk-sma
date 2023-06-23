/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma;

import cloud.cleo.chimesma.model.ResponseStartBotConversation.DialogActionType;
import java.time.LocalDate;
import software.amazon.awssdk.services.polly.model.VoiceId;

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
                .withKey("closing-en-US.wav")
                .build();

        final var speak3 = SpeakAction.builder()
                .withText("This is the last part")
                .withVoiceId(VoiceId.EMMA)
                //.withNextAction(hangup)
                .build();

        final var speak2 = SpeakAction.builder().
                withText("This is part 2")
                .withNextAction(speak3).build();

        final var speak = SpeakAction.builder()
                .withText("Welcome to the new age")
                .withVoiceId(VoiceId.SALLI)
                .withNextAction(speak2).build();

        final var start = PlayAudioAction.builder()
                .withKey("main.wav")
                .withNextAction(speak).build();

        final var hiddenMenu = SpeakAction.builder()
                .withText("You have reached the special Hidden menu!")
                .withVoiceId(VoiceId.SALLI)
                .withNextAction(start)
                .build();

        final var digits = ReceiveDigitsAction.builder()
                .withInBetweenDigitsDurationInMilliseconds(700)
                .withFlushDigitsDurationInMilliseconds(2000)
                .withInputDigitsRegex("^\\*\\*$")
                .withNextAction(start)
                .withDigitsRecevedAction(hiddenMenu).build();

        final var connect = ConnectCallAndBridgeAction.builder()
                .withUri("+15052162949")
                .withRingbackToneKey("transfer.wav")
                .build();

        final var call = CallAndBridgeAction.builder()
                .withUri("+16122260725")
                .withRingbackToneKey("transfer.wav")
                .build();

        final var weatherBot = StartBotConversationAction.builder()
                .withBotAliasArn(System.getenv("WEATHER_BOT_ALIAS_ARN"))
                .withContent("You are now at the weather lex bot")
                .withDialogActionType(DialogActionType.ElicitIntent)
                .build();

        final var chatGptBot = StartBotConversationAction.builder()
                .withBotAliasArn(System.getenv("CHATGPT_BOT_ALIAS_ARN"))
                .withContent("Go ahead ask Chat GPT anything")
                .withDialogActionType(DialogActionType.ElicitIntent)
                .build();

        var lexBot = StartBotConversationAction.builder()
                .withContent("Press One for Weather, Two for for Representative, or simply tell me how I can help?")
                .withDialogActionType(DialogActionType.ElicitIntent)
                .withNextAction(speak3)
                .withIntentMatcher(a -> {
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

        chatGptBot.setIntentMatcher(a -> {
            switch (a.getIntent()) {
                case "Quit":
                    return goodbye;
                default:
                    return lexBot;
            }
        });

        weatherBot.setIntentMatcher(a -> {
            switch (a.getIntent()) {
                case "Quit":
                    return goodbye;
                default:
                    return lexBot;
            }
        });

        final var region = SpeakAction.builder()
                .withText(f -> "The region is " + System.getenv("AWS_REGION").replace("-", " ").toUpperCase())
                .withVoiceId(VoiceId.SALLI)
                .withNextAction(lexBot)
                .build();

        final var precall = PlayAudioAction.builder()
                .withKey("main.wav")
                .withNextAction(region).build();

        return region;
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
