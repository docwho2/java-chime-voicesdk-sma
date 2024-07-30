package cloud.cleo.chimesma.examples.actions;

import cloud.cleo.chimesma.actions.*;
import cloud.cleo.chimesma.actions.SpeakAndGetDigitsAction.SpeechParameters;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import static java.time.temporal.ChronoUnit.MINUTES;
import java.util.Random;
import java.util.function.Function;

/**
 * Constructing some sample actions and routing examples
 *
 * @author sjensen
 */
public class ExampleActions extends AbstractFlow {

    /**
     * Examples of using static vs dynamic values
     *
     * @return an Initial Action
     */
    @Override
    protected Action getInitialAction() {

        // This is sent as plain Text 
        SpeakAction.builder()
                .withText("Hello you have a built a Voice Application!");

        // This is sent as SSML, the library detects <speak> and automatically sets the correct type 
        SpeakAction.builder()
                .withText("<speak>Hello<break/> you have a built a Voice Application!</speak>");

        /**
         * This will not generally work as expected, the flow is built at Lambda initialization, so this will be be the
         * time the lambda initialized, so the caller will hear the same time call after call assuming the Lambda
         * remains hot.
         */
        SpeakAction.builder()
                .withText("Hello the time is " + LocalTime.now().truncatedTo(MINUTES).format(DateTimeFormatter.ISO_LOCAL_TIME));

        /**
         * This will say the current time when the flow executes per call since it is evaluated in the flow
         */
        SpeakAction.builder()
                .withTextF(action -> "Hello the time is " + LocalTime.now().truncatedTo(MINUTES).format(DateTimeFormatter.ISO_LOCAL_TIME));

        /**
         * This is OK, no need to use a function because the region can never change
         */
        SpeakAction.builder()
                .withText("The region is " + System.getenv("AWS_REGION").replace("-", " ").toUpperCase());

        // https://docs.aws.amazon.com/chime-sdk/latest/dg/speak-and-get-digits.html
        SpeakAndGetDigitsAction.builder()
                .withMinNumberOfDigits(1)
                .withMaxNumberOfDigits(1)
                .withInputDigitsRegex("^[12]{1}$") // You must enter exactly 1 or 2 to proceed
                .withRepeat(2)
                .withSpeechParameters(SpeechParameters.builder().withText("Pleast enter One or Two").build())
                .withFailureSpeechParameters(SpeechParameters.builder().withText("Pleast try again").build())
                .withNextActionF((a) -> {
                    return switch (a.getReceivedDigits()) {
                        case "1" ->
                            SpeakAction.builder().withText("You Pressed One !").build();
                        case "2" ->
                            SpeakAction.builder().withText("You Pressed Two !").build();
                        default ->
                            SpeakAction.builder().withText("You Pressed Nothing !").build();
                    };
                });

        // Another variation
        SpeakAndGetDigitsAction.builder()
                .withMinNumberOfDigits(1)
                .withMaxNumberOfDigits(1)
                .withInputDigitsRegex("^[12]{1}$") // You must enter exactly 1 or 2 to proceed
                .withRepeat(2)
                .withSpeechParameters(SpeechParameters.builder().withText("Pleast enter One or Two").build())
                .withFailureSpeechParameters(SpeechParameters.builder().withText("Pleast try again").build())
                .withNextActionF((a) -> {
                    String pressed;
                    pressed = switch (a.getReceivedDigits()) {
                        case "1" ->
                            "one";
                        case "2" ->
                            "two";
                        default ->
                            "nothing";
                    };
                    return SpeakAction.builder().withTextF(ac -> "You Pressed " + pressed).build();
                });

        // Another more concise variation
        SpeakAndGetDigitsAction.builder()
                .withMinNumberOfDigits(1)
                .withMaxNumberOfDigits(1)
                .withInputDigitsRegex("^[12]{1}$") // You must enter exactly 1 or 2 to proceed
                .withRepeat(2)
                .withSpeechParameters(SpeechParameters.builder().withText("Pleast enter One or Two").build())
                .withFailureSpeechParameters(SpeechParameters.builder().withText("Pleast try again").build())
                .withNextActionF((a) -> {
                    return SpeakAction.builder().withTextF(ac -> "You Pressed " + (a.getReceivedDigits().isBlank() ? "Nothing" : a.getReceivedDigits())).build();
                });

        /**
         * The Library will use the Environment variable BOT_ALIAS_ARN by default, otherwise you would need to specify
         * it. https://docs.aws.amazon.com/chime-sdk/latest/dg/start-bot-conversation.html
         */
        StartBotConversationAction.builder()
                .withContent("Welcome to the Bot, how can I help you today?")
                .withNextActionF((a) -> {
                    return switch (a.getIntentName()) {
                        case "Quit" ->
                            SpeakAction.builder().withText("Thanks for calling, goodbye").build();
                        case "Transfer" ->
                            CallAndBridgeAction.builder().withArn("+18004444444").build();
                        default ->
                            getErrorAction();
                    }; // Didn't match any of the intents
                });

        /**
         * With your own ENV var
         */
        StartBotConversationAction.builder()
                .withContent("Welcome to the Bot, how can I help you today?")
                .withBotAliasArn(System.getenv("MY_SPECIAL_BOT_ARN"));

        /**
         * The Action itself is passed to all functions, so you have the full event model to query. So you can branch on
         * anything really, based on the call info or any Java logic
         */
        StartBotConversationAction.builder()
                .withContent("Welcome to the Bot, how can I help you today?")
                .withNextActionF((a) -> {
                    final var startTime = a.getEvent().getCallDetails().getParticipants().get(0).getStartTime();
                    if (Duration.between(startTime, Instant.now()).compareTo(Duration.ofMinutes(5)) > 0) {
                        // Caller is has been here over 5 minutes, eject them
                        return SpeakAction.builder().withText("You've been here to long")
                                .withNextAction(new HangupAction())
                                .build();
                    }

                    if (new Random().nextBoolean()) {
                        return SpeakAction.builder().withText("Sorry, you've randomly been selected to get hung up on")
                                .withNextAction(new HangupAction())
                                .build();
                    }
                    return switch (a.getIntentName()) {
                        case "Quit" ->
                            SpeakAction.builder().withText("Thanks for calling, goodbye").withNextAction(new HangupAction()).build();
                        case "Transfer" ->
                            CallAndBridgeAction.builder().withArn("+18004444444").build();
                        default ->
                            getErrorAction();
                    }; // Didn't match any of the intents
                });

        /**
         * Creating circular type flows requires you call a setter so you can reference yourself a
         */
        final var bot = StartBotConversationAction.builder()
                .withContent("Welcome to the Bot, how can I help you today?")
                .build();

        bot.setNextActionF((a) -> {
            return switch (a.getIntentName()) {
                case "Quit" ->
                    SpeakAction.builder().withText("Thanks for calling, goodbye").build();
                case "Transfer" ->
                    CallAndBridgeAction.builder().withArn("+18004444444").build();
                default ->
                    bot;
            }; // Didn't match any of the intents, keep them in the bot and start again
            // Only way out is to match intent or caller hangs up
            //self reference
        });

        /**
         * Use a class to encapsulate the routing decision
         */
        StartBotConversationAction.builder()
                .withContent("Welcome to the Bot, how can I help you today?")
                .withNextActionF(new LogicFunction());

        /**
         * Use a class that takes a parameter
         */
        SpeakAction.builder().withText("Thanks for calling, You will be hung up on or transferred")
                .withNextActionF(new HangupOrTransferFunction("+18004444444"));

        return new HangupAction();
    }

    /**
     * If you have allot of logic or need to reuse logic, then it makes sense to encapsulate in a class This class would
     * specific for handling Bot routing
     */
    public static class LogicFunction implements Function<StartBotConversationAction, Action> {

        @Override
        public Action apply(StartBotConversationAction action) {
            switch (action.getIntentName()) {
                case "Quit":
                    return SpeakAction.builder().withText("Thanks for calling, goodbye").build();
                case "Transfer":
                    return CallAndBridgeAction.builder().withArn("+18004444444").build();
                default:
                    return null;  // Null results in hangup
            }
        }

    }

    /**
     * Randomly Hang up or Transfer to a phone number after a a Speak Action
     *
     */
    public static class HangupOrTransferFunction implements Function<SpeakAction, Action> {

        private final String phoneNumber;

        public HangupOrTransferFunction(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        @Override
        public Action apply(SpeakAction action) {
            if (new Random().nextBoolean()) {
                return SpeakAction.builder().withText("Sorry, you've randomly been selected to get hung up on")
                        .withNextAction(new HangupAction())
                        .build();
            } else {
                return CallAndBridgeAction.builder().withArn(phoneNumber).build();
            }
        }

    }

    /**
     * Create 11 Pause Actions linked together. Used to test flow optimization
     *
     * @param nextAction
     * @return
     */
    public Action getPauseActions(Action nextAction) {
        // Build a set of Pause Actions to test library sending no more than 10 Actions
        final int eventCount = 11;
        Action startLoop = null;
        Action lastLoop = null;
        for (int i = 1; i <= eventCount; i++) {
            Action a = PauseAction.builder()
                    .withDescription("Pause " + i)
                    .withDurationInMilliseconds(100 + i)
                    .build();

            switch (i) {
                case 1 ->
                    startLoop = a;
                case eventCount -> {
                    lastLoop.setNextAction(a);
                    a.setNextAction(nextAction);
                }
                default ->
                    lastLoop.setNextAction(a);
            }
            lastLoop = a;
        }
        return startLoop;
    }

    @Override
    protected Action getErrorAction() {
        return null;
    }

    @Override
    protected void newCallHandler(Action action) {
        // Called when a new call comes in, execute any domain specific actions
    }

    @Override
    protected void hangupHandler(Action action) {
        // Called after the Hangup is received, execute any domain specific actions
    }

}
