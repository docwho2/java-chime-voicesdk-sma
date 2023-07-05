# Amazon Chime SDK SIP Media Application Java Library

## Background

This project provides a library to accelerate the development of [SIP Media Applications](https://docs.aws.amazon.com/chime-sdk/latest/ag/use-sip-apps.html) (SMA) in Java.
These apps allow for the deployment of multi-region and fault-tolerant self-service applications.  Some benefits of using Chime SDK versus 
[Amazon Connect](https://aws.amazon.com/pm/connect/) for self-service applications are:
- SIP Ingress
    - Bring your own carrier.
    - Load balance accross regions if desired.
- SIP Egress
    - Transfer calls to PBX's or other destinations via SIP for handling (PSTN bypass).
    - Use your preferred carrier to route calls or Amazon.
- Built from the ground up to be carrier class and multi-region.
- Amazon Chime PSTN numbers can span regions in the US (us-east-1 and us-west-2).
- Deployed as code versus Connect Flows.
    - Source control to manage and track changes.
    - Integrates into DevOps processes already in place.
- Central call control
    - Route calls to multiple Connect instances (many organizations have several instances and other groups on legacy systems).
    - Don't wrap/trombone calls through Connect instances when calls need to be transferred.
- Servicing calls in Chime SDK can potentially reduce costs.
    - SMA calls incur .002/min vs Connect .018/min for self-service.
    - PSTN Ingres and Egress charges are the same between Chime and Connect.

## Library Overview

The library provides two approaches, a JSON event mapping model, and on top of that a flow based library that lets programmers develop call flows 
without having to deal with event handling and flow optimization.  The latter allows developers to quickly build applications with minimal coding. 
See the Library [README](ChimeSMA/README.md) for more indepth information about using the library and writing applications.

### JSON Java Events

AWS Lambda is great at handling events in JSON.  AWS provides the [Lambda Java Events](https://github.com/aws/aws-lambda-java-libs/tree/main/aws-lambda-java-events) 
library to handle most of the services that directly integrate with Lambda and provides a full Java Object model for the requests and responses.  However the Chime SMA events are not included in this package.  This library is modeled after this approach and is used as follows:

- You define your Lambda to implement [RequestHandler](https://github.com/aws/aws-lambda-java-libs/blob/main/aws-lambda-java-core/src/main/java/com/amazonaws/services/lambda/runtime/RequestHandler.java)<[SMARequest](ChimeSMA/src/main/java/cloud/cleo/chimesma/model/SMARequest.java), [SMAResponse](ChimeSMA/src/main/java/cloud/cleo/chimesma/model/SMAResponse.java)> .
- Process the incoming request and repond as necessary, see [HellowWorld.java](Examples/src/main/java/cloud/cleo/chimesma/examples/response/HelloWorld.java) for an example.
- Note, you are responsible for handling the SMA state machine and this quickly becomes unamangeable as the complexity of what you are doing increases.
- Use this low-level approach when you need to control every aspect of the state machine.

### Java Action Flow Model

Building upon the above, the "Action Flow Model" maps each of the [supported actions for the PSTN Audio service](https://docs.aws.amazon.com/chime-sdk/latest/dg/specify-actions.html) 
to Java Objects that you dynamically connect to each other to create flows.  The Objects are easily extensible to allow the creation of complex interactions and routing. 
This part of the library provides:

- Flow based approach that makes developing applications easier to understand versus an event driven model.
    - The flow is built statically in memory at initializtion (SNAP Start Init) allowing for very low latency responses.
- Strong Java typing and builder approach for creating action objects.
- Sensible defaults are provided for most of the action parameters and can be set or passed into the Lambda (like BOT_ALIAS_ARN for example to start bot conversations) 
- Derived values whenever possible to reduce lines of code
    - Speak actions require you set SSML or Text for operations, the flow action takes just a text param and determines and sets the correct value when sending back the JSON.
- Response optimization sending multiple actions at once (SMA supports up to 10)
    - Each action is evaluated whether it can be chained with another before sending the action list.
    - Example: Pause -> Speak -> Pause -> StartRecording -> SpeakAndGetDigits sent one by one would require 4 Lambda calls without optimization.
    - Actions like SpeakAndGetDigits require a result before proceeding and cannot be chained with another action.
- Java Locale support across all relevant actions to easily build multi-lingual interactions (Prompts, Speak, and Bots)
- Extending existing actions is easy, see [CallAndBridgeActionTBTDiversion.java](Examples/src/main/java/cloud/cleo/chimesma/actions/CallAndBridgeActionTBTDiversion.java) which 
extends the standard [CallAndBridge](ChimeSMA/src/main/java/cloud/cleo/chimesma/actions/CallAndBridgeAction.java) action to write call info to a DyanmoDB table that can be used in a Connect flow to implement take back and transfer.  See use case later in this document.

To use the Flow Model:

- Simply create a Java Object that inherits from [AbstractFlow](ChimeSMA/src/main/java/cloud/cleo/chimesma/actions/AbstractFlow.java)
- Implement the getInitialAction() method that returns the start of the flow.
- See the flow based [HelloWorld.java](Examples/src/main/java/cloud/cleo/chimesma/examples/actions/HelloWorld.java) example.


## Amazon Connect Take Back and Transfer Use Case

This use case demonstrates sending calls to [Amazon Connect](https://aws.amazon.com/pm/connect/) and then later moving the call to some other destination 
like a PSTN number (which could be another Connect instance), SIP destination, or to continue a flow at the SMA Application.  Calls are released from
the Connect instance (call leg disconnected by the SMA Application) and moved to another destination.

The general steps are:
- [CallAndBridgeActionTBTDiversion](Examples/src/main/java/cloud/cleo/chimesma/actions/CallAndBridgeActionTBTDiversion.java) action writes call data to Dynamo DB table prior to actually moving the call.
- Connect Script executes a Lambda function to transfer calls



### High Level Components
![Architecture Diagram](assets/SMA-Connect-TBT.png)


### Connect Call Flow

In your connect flow, let's assume you have the destination number set on the contact attribute "TransferNumber".  Normally you would pass that
directly to "Transfer to phone number" step to place the outbound call.  For this use case, we simply insert a "Invoke AWS Lambda function" and "Wait"
step into the flow.  The orginal transfer step can be left in place as a failsafe. The wait condition gives the Lambda time to contact the Chime SDK API 
to signal the SMA app to disconnect the call.  Typically this all executes sub-second.

![Call Flow](assets/connectscript.png)

### Transfer Lambda

When invoking the Lambda from Connect, we pass both the Diversion header (Connect calls this "Call-Forwarding-Indicator") and the TransferNumber.


<img src="assets/lambdastep.png" width="30%" height="30%">

The lambda function then:
- Extracts the key from the SIP header (a random E164 number).
- Executes a Dynamo DB call to retrieve the call information which consists of: 
    - AWS Region
    - sipMediaApplicationId
    - transactionId
- Finally, executes a Chime SDK API call to UpdateSipMediaApplicationCall:
    - The region is used to initialize the client SDK properly to support calls ingressing from either region.
    - The TransferNumber is passed as a parameter so the SMA Handler knows where to transfer the call to.

Sample Lambda code in NodeJS.
  ```javascript
        const {ChimeClient, UpdateSipMediaApplicationCallCommand} = require("@aws-sdk/client-chime");
        const {DynamoDBClient, GetItemCommand} = require("@aws-sdk/client-dynamodb")
        const ddb = new DynamoDBClient();
        const regex = /(\+[0-9]+)/;
        const table_name = process.env.CALLS_TABLE_NAME;
        exports.handler = async function (event) {
            console.log(JSON.stringify(event));
            let match = event.Details.Parameters.Diversion.match(regex);
            console.log('Extracted phone is ' + match[1] );
            
            const dparams = {
                Key : {
                  phoneNumber : {
                    S: match[1]
                  }
                },
                TableName: table_name
            };
            
            const dresponse = await ddb.send(new GetItemCommand(dparams));
            console.log(JSON.stringify(dresponse))
            
            const cparams = {
                SipMediaApplicationId: dresponse.Item.sipMediaApplicationId.S,
                TransactionId: dresponse.Item.transactionId.S,
                Arguments: {
                    phoneNumber: event.Details.Parameters.TransferNumberD
                }   
            };
            // We need to know region before initializing client
            const chime = new ChimeClient({ region: dresponse.Item.region.S,  });
            const cresponse = await chime.send(new UpdateSipMediaApplicationCallCommand(cparams));
            console.log(JSON.stringify(cresponse));
            return {status: 'OK'};
        };
  ```

## Example Flow Application

The [Example Flow](Examples/src/main/java/cloud/cleo/chimesma/examples/actions/ExampleFlow.java) excercises a number of the 
Actions provided by the Chime SDK.  The Connect use case above is also incoporated into the Demo app.

### High Level Components

![Architecture Diagram](assets/ChimeSMA-Demo.png)

### Calling into the Demo Application

#### Twilio

[Twilio SIP Trunking](https://www.twilio.com/docs/sip-trunking) can be used to send calls into your SMA's or the SIP carrier of your choice.  
For this demo, the Twilio number of +1-320-495-2425 will be load balanced across regions.  The first prompt in the demo announces the region
so you can observe that calling the above number will land you in either us-east-1 or us-west-2.  When configuring the Twilio 
[Origination Settings](https://www.twilio.com/docs/sip-trunking#origination) you can make use of the "edge" setting to optimize the SIP traffic.  

In this case, the first SIP URI references a [Voice Connector](https://docs.aws.amazon.com/chime-sdk/latest/ag/voice-connectors.html) in the us-east-1 
region, so by adding the "edge=asburn" twilio will egress that call into AWS all within us-east-1.  The same applies for the "edge=umatilla" which is 
Twilio's edge in Oregon (us-west-2).  You don't want your traffic traversing all over the internet if that can be avoided.

![Twilio Origination Settings](assets/twilio.png)

#### Chime SDK Phone Number

After provisiong a [phone number in Chime](https://docs.aws.amazon.com/chime-sdk/latest/ag/provision-phone.html) ,
you create a [SIP Rule](https://docs.aws.amazon.com/chime-sdk/latest/ag/understand-sip-data-models.html) for the phone number.  Chime does not allow 
load balancing, so you must setup an ordered priority.  When you call +1-320-200-2007 you will always be routed the SMA in us-east-1, and only if 
that region or the Lambda associated with the SMA goes down, then you will fail over to us-west-2.

Chime does not provide PTSN numbers in all countries, only the US at present.  So if you are deploying in Europe, you will need to use a SIP carrier 
like Twilio above.  I have tested all this in Franfurt and London regions without issue.  See the PSTN section of the [Available Regions](https://docs.aws.amazon.com/chime-sdk/latest/dg/sdk-available-regions.html) 
documentation for Chime SDK. 

![Chime Phone Targets](assets/chimephonenumber.png)

#### Asterisk PBX

For testing apps there certainly is no reason to incur PSTN charges, so I use an IP Phone connected to [Asterisk](https://www.asterisk.org) to place 
calls into SMA's.  Like Twilio above, in the [pjsip_wizzard.conf](https://wiki.asterisk.org/wiki/display/AST/PJSIP+Configuration+Wizard) you can create trunks 
for each region endpoint:

```
[aws-chime-east]
type=wizard
transport=transport-udp
remote_hosts=cze9epizslzqslzjpo58ff.voiceconnector.chime.aws
endpoint/disallow=all
endpoint/allow=ulaw
endpoint/direct_media=no
endpoint/dtmf_mode=auto
endpoint/rtp_symmetric=yes

[aws-chime-oregon]
type=wizard
transport=transport-udp
remote_hosts=dnpz57kzlmo6uvhb1anu3w.voiceconnector.chime.aws
endpoint/disallow=all
endpoint/allow=ulaw
endpoint/direct_media=no
endpoint/dtmf_mode=auto
endpoint/rtp_symmetric=yes
```

You can observe no less than 12 endpoints are ready to take your call in each region !!!

```
Asterisk*CLI> pjsip show endpoint aws-chime-oregon 

Endpoint:  aws-chime-oregon                                     Not in use    0 of inf
        Aor:  aws-chime-oregon                                   0
      Contact:  aws-chime-oregon/sip:dnpz57kzlmo6uvhb1anu3 228c75f425 Created       0.000
  Transport:  transport-udp             udp      0      0  0.0.0.0:5060
   Identify:  aws-chime-oregon-identify/aws-chime-oregon
        Match: 99.77.253.106/32
        Match: 99.77.253.110/32
        Match: 99.77.253.109/32
        Match: 99.77.253.104/32
        Match: 99.77.253.102/32
        Match: 99.77.253.107/32
        Match: 99.77.253.103/32
        Match: 99.77.253.105/32
        Match: 99.77.253.11/32
        Match: 99.77.253.0/32
        Match: 99.77.253.108/32
        Match: 99.77.253.100/32
```

In the [extensions.conf](https://wiki.asterisk.org/wiki/display/AST/Contexts%2C+Extensions%2C+and+Priorities) you configure a number you can dial 
to route to the trunks in question.  The number  +1-703-555-0122 is a Chime Call in number than can be used to route to SMA's.  This allows you to 
call into connectors and your SMA with a sip rule without provisioning a phone number at all !

- 290 will try us-east-1 first and if it fails, you hear a prompt (so you know the first region was down) and then it tries the next region
- 291 will call only us-east-1
- 292 will call only us-west-2

```
exten => 290,1,NoOP(Call to AWS Chime with ordered failover)
        same => n,Dial(PJSIP/+17035550122@aws-chime-east)
        same => n,Playback(sorry-youre-having-problems)
        same => n,Dial(PJSIP/+17035550122@aws-chime-oregon)

exten => 291,1,NoOP(Call to AWS Chime East)
        same => n,Dial(PJSIP/+17035550122@aws-chime-east)

exten => 292,1,NoOP(Call to AWS Chime Oregon)
        same => n,Dial(PJSIP/+17035550122@aws-chime-oregon)
```


### Starting the Flow

The demo flow is broken into 2 main parts to demonstate moving up and down menu levels
- The main menu to handle top level functions
- Recording sub-menu to handle recording and playback of audio

The initial Action is to play a static prompt and proceed to the main menu

```java
public class ExampleFlow extends AbstractFlow {

    private final static Action MAIN_MENU = getMainMenu();
    private final static Action CALL_RECORDING_MENU = getCallRecordingMenu();
    @Override
    protected Action getInitialAction() {

        // Start with a welcome message and then main menu with region static prompt
        return PlayAudioAction.builder()
                .withKey(System.getenv("AWS_REGION") + "-welcome.wav") // This is always in english
                .withNextAction(MAIN_MENU)
                .withErrorAction(MAIN_MENU)
                .build();

    }

     public static Action getMainMenu() {
        // Main menu will be locale specific prompting
        final var menu = PlayAudioAndGetDigitsAction.builder()
                .withAudioSource(AudioSourceLocale.builder().withKeyLocale("main-menu").build())
                .withFailureAudioSource(AudioSourceLocale.builder().withKeyLocale("try-again").build())
                .withRepeatDurationInMilliseconds(3000)
                .withRepeat(2)
                .withMinNumberOfDigits(1)
                .withMaxNumberOfDigits(1)
                .withInputDigitsRegex("^\\d{1}$")
                .withErrorAction(goodbye)
                .withNextActionF(a -> {
                    switch (a.getReceivedDigits()) {
                        case "1":
                            return lexBotEN;
                        case "2":
                            return lexBotES;
                        case "3":
                            return connect;
                        case "4":
                            return CALL_RECORDING_MENU;
                        default:
                            return goodbye;
                    }
                })
                .build();
...
```

The initial response will make use of the [PlayAudio](https://docs.aws.amazon.com/chime-sdk/latest/dg/play-audio.html) and 
[PlayAudioAndGetDigits](https://docs.aws.amazon.com/chime-sdk/latest/dg/play-audio-get-digits.html) Actions.  These both make use
of static prompts stored in S3.  The main menu prompt is locale specific and can play in both English and Spanish as we will see later.  
These two actions are chainable so the library will send them both in the initial response:

```json
{
    "SchemaVersion": "1.0",
    "Actions": [
        {
            "Type": "PlayAudio",
            "Parameters": {
                "CallId": "be23f6a9-66f0-4d55-b6d9-b9e614a729ac",
                "ParticipantTag": "LEG-A",
                "AudioSource": {
                    "Type": "S3",
                    "BucketName": "chime-voicesdk-sma-promptbucket-1sr9bfy6k3k30",
                    "Key": "us-west-2-welcome.wav"
                }
            }
        },
        {
            "Type": "PlayAudioAndGetDigits",
            "Parameters": {
                "CallId": "be23f6a9-66f0-4d55-b6d9-b9e614a729ac",
                "ParticipantTag": "LEG-A",
                "InputDigitsRegex": "^\\d{1}$",
                "AudioSource": {
                    "Type": "S3",
                    "BucketName": "chime-voicesdk-sma-promptbucket-1sr9bfy6k3k30",
                    "Key": "main-menu-en-US.wav"
                },
                "FailureAudioSource": {
                    "Type": "S3",
                    "BucketName": "chime-voicesdk-sma-promptbucket-1sr9bfy6k3k30",
                    "Key": "try-again-en-US.wav"
                },
                "MinNumberOfDigits": 1,
                "MaxNumberOfDigits": 1,
                "Repeat": 2,
                "RepeatDurationInMilliseconds": 3000
            }
        }
    ],
    "TransactionAttributes": {
        "CurrentActionId": "6",
        "locale": "en-US",
        "CurrentActionIdList": "18,6"
    }
}
```

### Main Menu

The main menu has 4 choices
- Press 1 for Chat GPT Bot in English
- Press 2 for Chat GPT Bot in Spanish
- Press 3 for Connect Take Back and Transfer
- Press 4 for Audio Reocding Menu
- Another other key ends the call

After the caller enters a digit (or enters nothing and the timeout occurs) the below
code is executed to move the call to the next action.

```java
.withNextActionF(a -> {
    switch (a.getReceivedDigits()) {
        case "1":
            return lexBotEN;
        case "2":
            return lexBotES;
        case "3":
            return connect;
        case "4":
            return CALL_RECORDING_MENU;
        default:
            return goodbye;
    }
})
```

### Lex Bots

Handing control over to a Lex Bot is pretty easy in Chime just like in Amazon Connect.  We could have used a Bot to handle the main menu function 
rather than collecting digits and then further delegate intents to other bots or flows. In this example our Bot defined in the CloudFormation 
[template](template.yaml) has 3 intents:
- "Quit" - the caller is done talking to ChatGPT and wants to move on.
- "Transfer" - the caller wants to speak with a real person.
- "FallbackIntent" - anything else the caller asks is passed to ChatGPT for a response with context maintained in a DynamoDB table.

When the user presses:
- One, control is passed to our Bot with a locale set to English (en-US) and welcome prompt in English
- Two, control is passed to our Bot with a locale set to Spanish (es-US) and welcome prompt in Spanish

Anytime ".withLocale()" is used on an action, that state is maintained by the library.  You can observe this by:
- Pressing Two to go to the Spanish Bot
- Say "adiós" to tell the bot your done
- The next action will then be to go to the main menu, however you will now hear the main menu in Spanish because the locale was set on a prior action
- If you then pressed One it would set the locale back to English and when you return to the main menu it would be in English once again

Your Lex session is tied to the call ID so you can do the following:
- Press One for ChatGPT
- Say "What is the biggest lake in Minnesota?"
- ChatGPT:  "Lake Superior, anything else?"
- Say "That's all"
- Back at the main menu now
- Press One for ChatGPT again
- Say "How deep is that Lake?"
- ChatGPT: "Lake Superior is XXX feet deep, etc."

The Bot still knows the context you're in and what you said to ChatGPT during the call, so it knows when you come back that you are still referring to 
Lake Superior.  If you tell the bot you want to speak with a person it will return the "Transfer" intent back and the Next Action will be the Connect 
use case of take back and transfer which is the same as pressing 3 at the main menu.

```java
        final var lexBotEN = StartBotConversationAction.builder()
                .withDescription("ChatGPT English")
                .withLocale(english)
                .withContent("What can Chat GPT help you with?")
                .build();

        final var lexBotES = StartBotConversationAction.builder()
                .withDescription("ChatGPT Spanish")
                .withLocale(spanish)
                .withContent("¿En qué puede ayudarte Chat GPT?")
                .build();

        // Two invocations of the bot, so create one function and use for both
        Function<StartBotConversationAction, Action> botNextAction = (a) -> {
            switch (a.getIntentName()) {
                // The Lex bot also has intent to speak with someone
                case "Transfer":
                    return connect;
                case "Quit":
                default:
                    return MAIN_MENU;
            }
        };

        // Both bots are the same, so the handler is the same
        lexBotEN.setNextActionF(botNextAction);
        lexBotES.setNextActionF(botNextAction);
```

### Connect Take Back and Transfer

When pressing 3 or asking the Bot to speak with a person, the call will be transferred to an Amazon Connect instance in us-east-1.  This makes use 
of extending the base [CallAndBridge](https://docs.aws.amazon.com/chime-sdk/latest/dg/call-and-bridge.html) Action.  The details are described in the use 
case mentioned in a prior section.

Using this action in the library is no different than using the base [CallAndBridgeAction](ChimeSMA/src/main/java/cloud/cleo/chimesma/actions/CallAndBridgeAction.java).
In this case we are sending the call to a static number ("+15052162949") that points to sample call flow that executes the transfer Lambda and then 
transfers the call to "+18004444444" which is a carrier test number (Old MCI number).  This a terminal step, so once you have been transferred, you just
hangup to release the call resources in Chime.

```java
    // Send call to Connect to demo Take Back and Transfer
    final var connect = CallAndBridgeActionTBTDiversion.builder()
       .withDescription("Send Call to AWS Connect")
       .withUri("+15052162949")
       .withRingbackToneKeyLocale("transfer")  // If the Spanish locale is set, the caller will hear transferring in Spanish
       .build();
```

This Action could be further extended to to save the Locale in the DynamoDB Table and when the call lands at Connect, a lambda could be used to pull
this to set the language in the Connect flow or any other data collected in the Chime SMA app like caller account number, etc.

### Record Audio Menu

When pressing 4 at the main menu we go to a sub-menu that uses Speak instead of using static prompts like the main menu:
- Pressing one allows you to record audio which is saved to the default RECORD_BUCKET
- Pressing two plays back your recorded audio or a message indicating you have not recorded audio yet
- Any other key returns you back to the main menu.

```java
        // This menu is just in English, we  will use Speak instead of static prompts like main menu
        final var menu = SpeakAndGetDigitsAction.builder()
                .withSpeechParameters(SpeakAndGetDigitsAction.SpeechParameters.builder()
                        // Static text, but this could be dyanimc as well
                        .withText("Call Recording Menu. "
                                + "Press One to re cord an Audio File. "
                                + "Press Two to Listen to your recorded Audio File. "
                                + "Any other key to return to the Main Menu").build())
                .withFailureSpeechParameters(SpeakAndGetDigitsAction.SpeechParameters.builder()
                        .withText("Plese try again").build())
                .withRepeatDurationInMilliseconds(3000)
                .withRepeat(2)
                .withMinNumberOfDigits(1)
                .withMaxNumberOfDigits(1)
                .withInputDigitsRegex("^\\d{1}$")
                .withErrorAction(MAIN_MENU)
                .build();

        menu.setNextActionF(a -> {
            switch (a.getReceivedDigits()) {
                case "1":
                    return recordPrompt;
                case "2":
                    final var key = a.getTransactionAttribute(RecordAudioAction.RECORD_AUDIO_KEY);
                    if (key != null) {
                        // Some Audio has been recorded
                        return playAudio;
                    } else {
                        // No Audio has been recorded
                        return noRecording;
                    }
                default:
                    return MAIN_MENU;
            }
        });
```

The [RecordAudio](https://docs.aws.amazon.com/chime-sdk/latest/dg/record-audio.html) Action does not give you any indication to start recording so 
a beep wav file is used to indicate recording is active.  One again, the library optimizes the interaction by sending all the actions at once to 
create a fluid flow to the caller.  When pressing one to record audio, the SMA response would look like this.

```json
{
    "SchemaVersion": "1.0",
    "Actions": [
        {
            "Type": "Speak",
            "Parameters": {
                "Text": "At the beep, re cord up to 30 seconds of Audio.  Press any key to stop the recording.",
                "CallId": "f085191e-c647-4503-9d3a-3cba41aead2e",
                "LanguageCode": "en-US",
                "TextType": "text",
                "VoiceId": "Joanna"
            }
        },
        {
            "Type": "PlayAudio",
            "Parameters": {
                "CallId": "f085191e-c647-4503-9d3a-3cba41aead2e",
                "AudioSource": {
                    "Type": "S3",
                    "BucketName": "chime-voicesdk-sma-promptbucket-1sr9bfy6k3k30",
                    "Key": "beep.wav"
                }
            }
        },
        {
            "Type": "RecordAudio",
            "Parameters": {
                "CallId": "f085191e-c647-4503-9d3a-3cba41aead2e",
                "DurationInSeconds": 30,
                "SilenceDurationInSeconds": 5,
                "RecordingTerminators": [
                    "0","1","2","3","4","5","6","7","8","9","#","*"
                ],
                "RecordingDestination": {
                    "Type": "S3",
                    "BucketName": "chime-voicesdk-sma-recordbucket-rfr6d796zj6i"
                }
            }
        }
    ],
    "TransactionAttributes": {
        "CurrentActionId": "13",
        "locale": "en-US",
        "CurrentActionIdList": "15,14,13"
    }
}
```

### Wrapping Up

This library in combination with the CloudFormation template demonstrates:
- Deploying resilant mult-region voice applications
- Creation of static prompts with Polly at deploy time to save cost on Polly usage ([PollyPromptCreator](PollyPromptCreation/src/main/java/cloud/cleo/chimesma/PollyPromptGenerator.java))
- Deployment of static prompt files with the applicattion ([PromptCopier](PollyPromptCreation/src/main/java/cloud/cleo/chimesma/PromptCopier.java))
- Easily pipelined with "sam deploy" tied to source control
- Programming flows in Java that are easy to understand versus SMA Event handling model
- Easy to extend actions that can be used to keep flows concise and still easy to understand (Connect TBT for example)
- Utilizing Lambda SNAP Start to improve latency and performance (Flows are built at SNAP initialization)
- Locale support to easily support multi-lingual applications
- Utilize the full power of Java anywhere to make routing decisions or input/output to the Actions.

Things I may add to the demo in the future:
- Transfer Action that uses a Global DynamoDB Table
    - Maps logical names to transfer destinations
    - destinations might be SIP or PSTN
    - destinations might have other info like "Queue" or "Skill" name that would inserted into the transfer table for Connect to pick up
- Time of Day Action
    - pull hours from API or Dyanmo Table
    - Open/Closed ouputs
    - Holiday/Special hours
- [Function Calling](https://platform.openai.com/docs/guides/gpt/function-calling) to make the ChatGPT bot do more


## Deploy the Project

The Serverless Application Model Command Line Interface (SAM CLI) is an extension of the AWS CLI that adds functionality for building and testing Lambda applications.  
Before proceeding, it is assumed you have valid AWS credentials setup with the AWS CLI and permissions to perform CloudFormation stack operations.

To use the SAM CLI, you need the following tools.

* SAM CLI - [Install the SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
* Java11 - [Install the Java 11](https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/downloads-list.html)
* Maven - [Install Maven](https://maven.apache.org/install.html)

If you have brew installed then
```bash
brew install aws-sam-cli
brew install corretto11
brew install maven
```

To build and deploy, run the following in your shell.  Note: you must edit the [samconfig.toml](samconfig.toml) and change the parameteres to 
taste before running the build like the Connect Instance ID and SMA ID to ones that exist within that region.

```bash
git clone https://github.com/docwho2/java-chime-voicesdk-sma.git
cd java-chime-voicesdk-sma
./init.bash
sam build
sam deploy --config-env east
sam deploy --config-env west
```

The commands perform the follwoing operations:
- Clones the repository into your local directory
- Change directory into the cloned repository
- Set up some required components like the V4 Java Events library that is not published yet (this is a sub-module) and install the parent POM used by Lambda functions.
- Build the components that will be deployed by SAM
- Package and deploy the project to us-east-1
- Package and deploy the project to us-west-2

You will see the progress as the stack deploys.  As metntioned earlier, you will need to put your OpenAI API Key into parameter store or the deploy will error, but it will give you an error message 
that tells you there is no value for "OPENAI_API_KEY" in the [Parameter Store](https://docs.aws.amazon.com/systems-manager/latest/userguide/systems-manager-parameter-store.html).



## Fetch, tail, and filter Lambda function logs

To simplify troubleshooting, SAM CLI has a command called `sam logs`. `sam logs` lets you fetch logs generated by the deployed Lambda functions from the command line. In addition to printing the logs on the terminal, this command has several nifty features to help you quickly see what's going on with the demo.


```bash
sam logs --tail
```

Example:
```
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:34.013000 RESTORE_START Runtime Version: java:11.v21	Runtime Version ARN: arn:aws:lambda:us-east-1::runtime:156ab0dc268a6b4a8dedcbcf0974795cafba2ee8760fe386062fffdbb887b971
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:34.376000 RESTORE_REPORT Restore Duration: 511.25 ms
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:34.379000 START RequestId: b771fecc-1b53-4faf-922d-1d74357b1676 Version: 56
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:34.565000 b771fecc-1b53-4faf-922d-1d74357b1676 DEBUG AbstractFlow:214 - SMARequest(schemaVersion=1.0, sequence=1, invocationEventType=NEW_INBOUND_CALL, callDetails=SMARequest.CallDetails(transactionId=6600de06-fc5a-4a57-8c11-420ccae6f93b, transactionAttributes=null, awsAccountId=364253738352, awsRegion=us-east-1, sipMediaApplicationId=cf3e17cd-f4e5-44c3-ab04-325e6b3a6709, participants=[SMARequest.Participant(callId=6cbd7153-b1cd-48b1-8598-9687f6903db1, participantTag=LEG-A, to=+17035550122, from=+16128140714, direction=Inbound, startTime=2023-07-05T10:16:33.239Z, status=null)]), errorType=null, errorMessage=null, actionData=null)
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:34.566000 b771fecc-1b53-4faf-922d-1d74357b1676 DEBUG AbstractFlow:219 - New Inbound Call, starting flow
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:34.567000 b771fecc-1b53-4faf-922d-1d74357b1676 INFO  AbstractFlow:149 - Adding action PlayAudio key=[us-east-1-welcome.wav] bucket=[chime-voicesdk-sma-promptbucket-1p1tvnc4izve]
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:34.567000 b771fecc-1b53-4faf-922d-1d74357b1676 INFO  AbstractFlow:157 - Chaining action PlayAudioAndGetDigits [^\d{1}$]
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:34.595000 b771fecc-1b53-4faf-922d-1d74357b1676 INFO  AbstractFlow:238 - New Call Handler Code Here
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:34.682000 b771fecc-1b53-4faf-922d-1d74357b1676 DEBUG AbstractFlow:314 - {"SchemaVersion":"1.0","Actions":[{"Type":"PlayAudio","Parameters":{"CallId":"6cbd7153-b1cd-48b1-8598-9687f6903db1","ParticipantTag":"LEG-A","AudioSource":{"Type":"S3","BucketName":"chime-voicesdk-sma-promptbucket-1p1tvnc4izve","Key":"us-east-1-welcome.wav"}}},{"Type":"PlayAudioAndGetDigits","Parameters":{"CallId":"6cbd7153-b1cd-48b1-8598-9687f6903db1","ParticipantTag":"LEG-A","InputDigitsRegex":"^\\d{1}$","AudioSource":{"Type":"S3","BucketName":"chime-voicesdk-sma-promptbucket-1p1tvnc4izve","Key":"main-menu-en-US.wav"},"FailureAudioSource":{"Type":"S3","BucketName":"chime-voicesdk-sma-promptbucket-1p1tvnc4izve","Key":"try-again-en-US.wav"},"MinNumberOfDigits":1,"MaxNumberOfDigits":1,"Repeat":2,"RepeatDurationInMilliseconds":3000}}],"TransactionAttributes":{"CurrentActionId":"6","locale":"en-US","CurrentActionIdList":"18,6"}}
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:34.686000 END RequestId: b771fecc-1b53-4faf-922d-1d74357b1676
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:34.686000 REPORT RequestId: b771fecc-1b53-4faf-922d-1d74357b1676	Duration: 306.34 ms	Billed Duration: 610 ms	Memory Size: 3009 MB	Max Memory Used: 155 MB	Restore Duration: 511.25 ms	Billed Restore Duration: 303 ms	
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:43.928000 START RequestId: 7667964c-05ac-4891-b28c-56f1282ebc1b Version: 56
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:44.025000 7667964c-05ac-4891-b28c-56f1282ebc1b DEBUG AbstractFlow:214 - SMARequest(schemaVersion=1.0, sequence=2, invocationEventType=ACTION_SUCCESSFUL, callDetails=SMARequest.CallDetails(transactionId=6600de06-fc5a-4a57-8c11-420ccae6f93b, transactionAttributes={CurrentActionId=6, locale=en-US, CurrentActionIdList=18,6}, awsAccountId=364253738352, awsRegion=us-east-1, sipMediaApplicationId=cf3e17cd-f4e5-44c3-ab04-325e6b3a6709, participants=[SMARequest.Participant(callId=6cbd7153-b1cd-48b1-8598-9687f6903db1, participantTag=LEG-A, to=+17035550122, from=+16128140714, direction=Inbound, startTime=2023-07-05T10:16:33.239Z, status=Connected)]), errorType=null, errorMessage=null, actionData=ResponsePlayAudioAndGetDigits(type=PlayAudioAndGetDigits, parameters=ResponsePlayAudioAndGetDigits.Parameters(callId=6cbd7153-b1cd-48b1-8598-9687f6903db1, participantTag=LEG-A, inputDigitsRegex=^\d{1}$, audioSource=ResponsePlayAudio.AudioSource(type=S3, bucketName=chime-voicesdk-sma-promptbucket-1p1tvnc4izve, key=main-menu-en-US.wav), failureAudioSource=ResponsePlayAudio.AudioSource(type=S3, bucketName=chime-voicesdk-sma-promptbucket-1p1tvnc4izve, key=try-again-en-US.wav), minNumberOfDigits=1, maxNumberOfDigits=1, terminatorDigits=null, inBetweenDigitsDurationInMilliseconds=3000, repeat=2, repeatDurationInMilliseconds=3000), receivedDigits=1, errorType=null, errorMessage=null))
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:44.029000 7667964c-05ac-4891-b28c-56f1282ebc1b DEBUG AbstractFlow:207 - Current Action is PlayAudioAndGetDigits [^\d{1}$] with ID 6
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:44.030000 7667964c-05ac-4891-b28c-56f1282ebc1b DEBUG Action:180 - This Action has a locale set to en_US
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:44.031000 7667964c-05ac-4891-b28c-56f1282ebc1b INFO  AbstractFlow:149 - Adding action StartBotConversation desc=[ChatGPT English] da=[ElicitIntent] content=[What can Chat GPT help you with?]
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:44.045000 7667964c-05ac-4891-b28c-56f1282ebc1b INFO  AbstractFlow:340 - Moving to next action: StartBotConversation desc=[ChatGPT English] da=[ElicitIntent] content=[What can Chat GPT help you with?]
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:44.065000 7667964c-05ac-4891-b28c-56f1282ebc1b DEBUG AbstractFlow:314 - {"SchemaVersion":"1.0","Actions":[{"Type":"StartBotConversation","Parameters":{"CallId":"6cbd7153-b1cd-48b1-8598-9687f6903db1","BotAliasArn":"arn:aws:lex:us-east-1:364253738352:bot-alias/GDGCNIR2DC/NMJJX2WV6A","LocaleId":"en_US","Configuration":{"SessionState":{"DialogAction":{"Type":"ElicitIntent"}},"WelcomeMessages":[{"Content":"What can Chat GPT help you with?","ContentType":"PlainText"}]}}}],"TransactionAttributes":{"CurrentActionId":"4","locale":"en-US","CurrentActionIdList":"4"}}
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:44.067000 END RequestId: 7667964c-05ac-4891-b28c-56f1282ebc1b
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:44.067000 REPORT RequestId: 7667964c-05ac-4891-b28c-56f1282ebc1b	Duration: 138.27 ms	Billed Duration: 139 ms	Memory Size: 3009 MB	Max Memory Used: 159 MB	
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:55.980000 START RequestId: c5617d6b-3eff-409b-bff8-7f6ed83c319a Version: 56
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:56.025000 c5617d6b-3eff-409b-bff8-7f6ed83c319a DEBUG AbstractFlow:214 - SMARequest(schemaVersion=1.0, sequence=3, invocationEventType=ACTION_SUCCESSFUL, callDetails=SMARequest.CallDetails(transactionId=6600de06-fc5a-4a57-8c11-420ccae6f93b, transactionAttributes={CurrentActionId=4, locale=en-US, CurrentActionIdList=4}, awsAccountId=364253738352, awsRegion=us-east-1, sipMediaApplicationId=cf3e17cd-f4e5-44c3-ab04-325e6b3a6709, participants=[SMARequest.Participant(callId=6cbd7153-b1cd-48b1-8598-9687f6903db1, participantTag=LEG-A, to=+17035550122, from=+16128140714, direction=Inbound, startTime=2023-07-05T10:16:33.239Z, status=Connected)]), errorType=null, errorMessage=null, actionData=ActionDataStartBotConversation(callId=null, type=StartBotConversation, parameters=ResponseStartBotConversation.Parameters(callId=6cbd7153-b1cd-48b1-8598-9687f6903db1, participantTag=LEG-A, botAliasArn=arn:aws:lex:us-east-1:364253738352:bot-alias/GDGCNIR2DC/NMJJX2WV6A, localeId=en_US, configuration=ResponseStartBotConversation.Configuration(sessionState=ResponseStartBotConversation.SessionState(sessionAttributes=null, dialogAction=ResponseStartBotConversation.DialogAction(type=ElicitIntent), intent=null), welcomeMessages=[ResponseStartBotConversation.WelcomeMessage(content=What can Chat GPT help you with?, contentType=PlainText)])), intentResult=ActionDataStartBotConversation.IntentResult(sessionId=6cbd7153-b1cd-48b1-8598-9687f6903db1, sessionState=ResponseStartBotConversation.SessionState(sessionAttributes={}, dialogAction=null, intent=ResponseStartBotConversation.Intent(name=Quit, Slots={}, state=ReadyForFulfillment, confirmationState=None)), interpretations=[ActionDataStartBotConversation.Interpretation(intent=ResponseStartBotConversation.Intent(name=Quit, Slots={}, state=ReadyForFulfillment, confirmationState=None), nluConfidence=ActionDataStartBotConversation.NluConfidence(score=1.0)), ActionDataStartBotConversation.Interpretation(intent=ResponseStartBotConversation.Intent(name=FallbackIntent, Slots={}, state=null, confirmationState=null), nluConfidence=null), ActionDataStartBotConversation.Interpretation(intent=ResponseStartBotConversation.Intent(name=Transfer, Slots={}, state=null, confirmationState=null), nluConfidence=ActionDataStartBotConversation.NluConfidence(score=0.42))]), errorType=null, errorMessage=null))
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:56.026000 c5617d6b-3eff-409b-bff8-7f6ed83c319a DEBUG Action:180 - This Action has a locale set to en_US
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:56.026000 c5617d6b-3eff-409b-bff8-7f6ed83c319a DEBUG AbstractFlow:207 - Current Action is StartBotConversation desc=[ChatGPT English] da=[ElicitIntent] content=[What can Chat GPT help you with?] with ID 4
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:56.026000 c5617d6b-3eff-409b-bff8-7f6ed83c319a DEBUG Action:99 - Lex Bot has finished and Intent is Quit
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:56.027000 c5617d6b-3eff-409b-bff8-7f6ed83c319a INFO  AbstractFlow:149 - Adding action PlayAudioAndGetDigits [^\d{1}$]
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:56.027000 c5617d6b-3eff-409b-bff8-7f6ed83c319a INFO  AbstractFlow:340 - Moving to next action: PlayAudioAndGetDigits [^\d{1}$]
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:56.028000 c5617d6b-3eff-409b-bff8-7f6ed83c319a DEBUG AbstractFlow:314 - {"SchemaVersion":"1.0","Actions":[{"Type":"PlayAudioAndGetDigits","Parameters":{"CallId":"6cbd7153-b1cd-48b1-8598-9687f6903db1","ParticipantTag":"LEG-A","InputDigitsRegex":"^\\d{1}$","AudioSource":{"Type":"S3","BucketName":"chime-voicesdk-sma-promptbucket-1p1tvnc4izve","Key":"main-menu-en-US.wav"},"FailureAudioSource":{"Type":"S3","BucketName":"chime-voicesdk-sma-promptbucket-1p1tvnc4izve","Key":"try-again-en-US.wav"},"MinNumberOfDigits":1,"MaxNumberOfDigits":1,"Repeat":2,"RepeatDurationInMilliseconds":3000}}],"TransactionAttributes":{"CurrentActionId":"6","locale":"en-US","CurrentActionIdList":"6","LexLastMatchedIntent":"Quit"}}
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:56.031000 END RequestId: c5617d6b-3eff-409b-bff8-7f6ed83c319a
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:56.031000 REPORT RequestId: c5617d6b-3eff-409b-bff8-7f6ed83c319a	Duration: 50.76 ms	Billed Duration: 51 ms	Memory Size: 3009 MB	Max Memory Used: 159 MB	
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:58.051000 START RequestId: 09cea32b-12e6-4283-9092-15e3fd5eabf8 Version: 56
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:58.054000 09cea32b-12e6-4283-9092-15e3fd5eabf8 DEBUG AbstractFlow:214 - SMARequest(schemaVersion=1.0, sequence=4, invocationEventType=ACTION_SUCCESSFUL, callDetails=SMARequest.CallDetails(transactionId=6600de06-fc5a-4a57-8c11-420ccae6f93b, transactionAttributes={CurrentActionId=6, LexLastMatchedIntent=Quit, locale=en-US, CurrentActionIdList=6}, awsAccountId=364253738352, awsRegion=us-east-1, sipMediaApplicationId=cf3e17cd-f4e5-44c3-ab04-325e6b3a6709, participants=[SMARequest.Participant(callId=6cbd7153-b1cd-48b1-8598-9687f6903db1, participantTag=LEG-A, to=+17035550122, from=+16128140714, direction=Inbound, startTime=2023-07-05T10:16:33.239Z, status=Connected)]), errorType=null, errorMessage=null, actionData=ResponsePlayAudioAndGetDigits(type=PlayAudioAndGetDigits, parameters=ResponsePlayAudioAndGetDigits.Parameters(callId=6cbd7153-b1cd-48b1-8598-9687f6903db1, participantTag=LEG-A, inputDigitsRegex=^\d{1}$, audioSource=ResponsePlayAudio.AudioSource(type=S3, bucketName=chime-voicesdk-sma-promptbucket-1p1tvnc4izve, key=main-menu-en-US.wav), failureAudioSource=ResponsePlayAudio.AudioSource(type=S3, bucketName=chime-voicesdk-sma-promptbucket-1p1tvnc4izve, key=try-again-en-US.wav), minNumberOfDigits=1, maxNumberOfDigits=1, terminatorDigits=null, inBetweenDigitsDurationInMilliseconds=3000, repeat=2, repeatDurationInMilliseconds=3000), receivedDigits=8, errorType=null, errorMessage=null))
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:58.054000 09cea32b-12e6-4283-9092-15e3fd5eabf8 DEBUG AbstractFlow:207 - Current Action is PlayAudioAndGetDigits [^\d{1}$] with ID 6
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:58.054000 09cea32b-12e6-4283-9092-15e3fd5eabf8 INFO  AbstractFlow:149 - Adding action PlayAudio desc=[Say Goodbye] keyL=[goodbye] bucket=[chime-voicesdk-sma-promptbucket-1p1tvnc4izve]
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:58.055000 09cea32b-12e6-4283-9092-15e3fd5eabf8 INFO  AbstractFlow:157 - Chaining action Hangup desc=[This is my last step]
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:58.057000 09cea32b-12e6-4283-9092-15e3fd5eabf8 INFO  AbstractFlow:340 - Moving to next action: PlayAudio desc=[Say Goodbye] keyL=[goodbye] bucket=[chime-voicesdk-sma-promptbucket-1p1tvnc4izve]
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:58.060000 09cea32b-12e6-4283-9092-15e3fd5eabf8 DEBUG AbstractFlow:314 - {"SchemaVersion":"1.0","Actions":[{"Type":"PlayAudio","Parameters":{"CallId":"6cbd7153-b1cd-48b1-8598-9687f6903db1","ParticipantTag":"LEG-A","AudioSource":{"Type":"S3","BucketName":"chime-voicesdk-sma-promptbucket-1p1tvnc4izve","Key":"goodbye-en-US.wav"}}},{"Type":"Hangup","Parameters":{"CallId":"6cbd7153-b1cd-48b1-8598-9687f6903db1"}}],"TransactionAttributes":{"CurrentActionId":"1","LexLastMatchedIntent":"Quit","locale":"en-US","CurrentActionIdList":"2,1"}}
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:58.061000 END RequestId: 09cea32b-12e6-4283-9092-15e3fd5eabf8
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:16:58.061000 REPORT RequestId: 09cea32b-12e6-4283-9092-15e3fd5eabf8	Duration: 10.09 ms	Billed Duration: 11 ms	Memory Size: 3009 MB	Max Memory Used: 160 MB	
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:17:00.009000 START RequestId: 010f6002-d185-4e96-9971-5360a2c6aa72 Version: 56
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:17:00.014000 010f6002-d185-4e96-9971-5360a2c6aa72 DEBUG AbstractFlow:214 - SMARequest(schemaVersion=1.0, sequence=5, invocationEventType=HANGUP, callDetails=SMARequest.CallDetails(transactionId=6600de06-fc5a-4a57-8c11-420ccae6f93b, transactionAttributes={CurrentActionId=1, LexLastMatchedIntent=Quit, locale=en-US, CurrentActionIdList=2,1}, awsAccountId=364253738352, awsRegion=us-east-1, sipMediaApplicationId=cf3e17cd-f4e5-44c3-ab04-325e6b3a6709, participants=[SMARequest.Participant(callId=6cbd7153-b1cd-48b1-8598-9687f6903db1, participantTag=LEG-A, to=+17035550122, from=+16128140714, direction=Inbound, startTime=2023-07-05T10:16:33.239Z, status=Disconnected)]), errorType=null, errorMessage=null, actionData=ResponseHangup(type=Hangup, parameters=ResponseHangup.Parameters(callId=6cbd7153-b1cd-48b1-8598-9687f6903db1, participantTag=LEG-A, sipResponseCode=null)))
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:17:00.015000 010f6002-d185-4e96-9971-5360a2c6aa72 DEBUG AbstractFlow:270 - Call Was disconnected by [Application], sending empty response
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:17:00.015000 010f6002-d185-4e96-9971-5360a2c6aa72 DEBUG AbstractFlow:207 - Current Action is Hangup desc=[This is my last step] with ID 1
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:17:00.015000 010f6002-d185-4e96-9971-5360a2c6aa72 INFO  AbstractFlow:243 - Hangup Handler Code Here
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:17:00.016000 010f6002-d185-4e96-9971-5360a2c6aa72 DEBUG AbstractFlow:314 - {"SchemaVersion":"1.0","Actions":[]}
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:17:00.017000 END RequestId: 010f6002-d185-4e96-9971-5360a2c6aa72
2023/07/05/[56]374cd7798d2a4e36a494a23d658b9741 2023-07-05T10:17:00.017000 REPORT RequestId: 010f6002-d185-4e96-9971-5360a2c6aa72	Duration: 8.31 ms	Billed Duration: 9 ms	Memory Size: 3009 MB	Max Memory Used: 160 MB	

^C CTRL+C received, cancelling...                                              
```

You can find more information and examples about filtering Lambda function logs in the [SAM CLI Documentation](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-logging.html).


## Cleanup

To delete the demo, use the SAM CLI.

You can run the following:

```bash
sam delete --config-env east
sam delete --config-env west
```

## Sample Deploy Output
```
java-chime-voicesdk-sma$ sam deploy --config-env west

		Managed S3 bucket: aws-sam-cli-managed-default-samclisourcebucket-13jnbeuzx2
		A different default S3 bucket can be set in samconfig.toml
		Or by specifying --s3-bucket explicitly.
	Uploading to chime-voicesdk-sma/0934badf714b3b6d4be6b4716f73d980  17401808 / 17401808  (100.00%)
File with same data already exists at chime-voicesdk-sma/0934badf714b3b6d4be6b4716f73d980, skipping upload                                        
	Uploading to chime-voicesdk-sma/3dd27d8c98d6bee8bdcca98c26123f26  14703860 / 14703860  (100.00%)
	Uploading to chime-voicesdk-sma/2eac5038f204c6de13a836fb2da6efb9  22977524 / 22977524  (100.00%)

	Deploying with following values
	===============================
	Stack name                   : chime-voicesdk-sma
	Region                       : us-west-2
	Confirm changeset            : False
	Disable rollback             : False
	Deployment s3 bucket         : aws-sam-cli-managed-default-samclisourcebucket-13jnbug4euzx2
	Capabilities                 : ["CAPABILITY_IAM"]
	Parameter overrides          : {"SMAID": "f6fb2553-e7e0-4900-866b-1b51b91f575a", "CONNECTID": "e8fac445-d291-407e-8fd7-c6296395c2ab"}
	Signing Profiles             : {}

Initiating deployment
=====================

	Uploading to chime-voicesdk-sma/93e0edbe536d96ab0deced939610d637.template  22349 / 22349  (100.00%)


Waiting for changeset to be created..

CloudFormation stack changeset
---------------------------------------------------------------------------------------------------------------------------------------------
Operation                           LogicalResourceId                   ResourceType                        Replacement                       
---------------------------------------------------------------------------------------------------------------------------------------------
+ Add                               BotAliasGPT                         AWS::Lex::BotAlias                  N/A                               
+ Add                               BotRuntimeRole                      AWS::IAM::Role                      N/A                               
+ Add                               BotVersionGPT                       AWS::Lex::BotVersion                N/A                               
+ Add                               ChatGPTAliasSNAPSTART               AWS::Lambda::Alias                  N/A                               
+ Add                               ChatGPTRole                         AWS::IAM::Role                      N/A                               
+ Add                               ChatGPTVersion3d508bab8c            AWS::Lambda::Version                N/A                               
+ Add                               ChatGPT                             AWS::Lambda::Function               N/A                               
+ Add                               ChimeCallLexGPT                     AWS::Lex::ResourcePolicy            N/A                               
+ Add                               ChimePolicy                         AWS::IAM::ManagedPolicy             N/A                               
+ Add                               ChimeSMAPerm                        AWS::Lambda::Permission             N/A                               
+ Add                               ChimeSMARole                        AWS::IAM::Role                      N/A                               
+ Add                               ChimeSMA                            AWS::Lambda::Function               N/A                               
+ Add                               GoodbyePromptEN                     Custom::PromptCreator               N/A                               
+ Add                               GoodbyePromptES                     Custom::PromptCreator               N/A                               
+ Add                               LexBotGPT                           AWS::Lex::Bot                       N/A                               
+ Add                               LexToChatGPTPerm                    AWS::Lambda::Permission             N/A                               
+ Add                               LexToChatGPTSnapPerm                AWS::Lambda::Permission             N/A                               
+ Add                               MainMenuEN                          Custom::PromptCreator               N/A                               
+ Add                               MainMenuES                          Custom::PromptCreator               N/A                               
+ Add                               MainPromptEast                      Custom::PromptCreator               N/A                               
+ Add                               MainPromptWest                      Custom::PromptCreator               N/A                               
+ Add                               PromptBucketPolicy                  AWS::S3::BucketPolicy               N/A                               
+ Add                               PromptBucket                        AWS::S3::Bucket                     N/A                               
+ Add                               PromptCopierRole                    AWS::IAM::Role                      N/A                               
+ Add                               PromptCopier                        AWS::Lambda::Function               N/A                               
+ Add                               PromptCreatorRole                   AWS::IAM::Role                      N/A                               
+ Add                               PromptCreator                       AWS::Lambda::Function               N/A                               
+ Add                               RecordBucketPolicy                  AWS::S3::BucketPolicy               N/A                               
+ Add                               RecordBucket                        AWS::S3::Bucket                     N/A                               
+ Add                               SessionTable                        AWS::DynamoDB::Table                N/A                               
+ Add                               StaticPrompts                       Custom::PromptCopier                N/A                               
+ Add                               TansferPromptEN                     Custom::PromptCreator               N/A                               
+ Add                               TansferPromptES                     Custom::PromptCreator               N/A                               
+ Add                               TransferCallConnectIntegration      AWS::Connect::IntegrationAssociat   N/A                               
                                                                        ion                                                                   
+ Add                               TransferCallRole                    AWS::IAM::Role                      N/A                               
+ Add                               TransferCall                        AWS::Lambda::Function               N/A                               
+ Add                               TryAgainEN                          Custom::PromptCreator               N/A                               
+ Add                               TryAgainES                          Custom::PromptCreator               N/A                               
---------------------------------------------------------------------------------------------------------------------------------------------


Changeset created successfully. arn:aws:cloudformation:us-west-2:changeSet/samcli-deploy1688419429/b28c26d0-12a8-4efc-a9af-aa49d9c404c9


2023-07-03 16:24:07 - Waiting for stack create/update to complete

CloudFormation events from stack operations (refresh every 5.0 seconds)
---------------------------------------------------------------------------------------------------------------------------------------------
ResourceStatus                      ResourceType                        LogicalResourceId                   ResourceStatusReason              
---------------------------------------------------------------------------------------------------------------------------------------------
CREATE_IN_PROGRESS                  AWS::IAM::Role                      BotRuntimeRole                      -                                 
CREATE_IN_PROGRESS                  AWS::DynamoDB::Table                SessionTable                        -                                 
CREATE_IN_PROGRESS                  AWS::S3::Bucket                     RecordBucket                        -                                 
CREATE_IN_PROGRESS                  AWS::IAM::ManagedPolicy             ChimePolicy                         -                                 
CREATE_IN_PROGRESS                  AWS::IAM::Role                      ChimeSMARole                        -                                 
CREATE_IN_PROGRESS                  AWS::S3::Bucket                     PromptBucket                        -                                 
CREATE_IN_PROGRESS                  AWS::IAM::Role                      BotRuntimeRole                      Resource creation Initiated       
CREATE_IN_PROGRESS                  AWS::IAM::ManagedPolicy             ChimePolicy                         Resource creation Initiated       
CREATE_IN_PROGRESS                  AWS::IAM::Role                      ChimeSMARole                        Resource creation Initiated       
CREATE_IN_PROGRESS                  AWS::S3::Bucket                     RecordBucket                        Resource creation Initiated       
CREATE_IN_PROGRESS                  AWS::S3::Bucket                     PromptBucket                        Resource creation Initiated       
CREATE_IN_PROGRESS                  AWS::DynamoDB::Table                SessionTable                        Resource creation Initiated       
CREATE_COMPLETE                     AWS::DynamoDB::Table                SessionTable                        -                                 
CREATE_IN_PROGRESS                  AWS::IAM::Role                      ChatGPTRole                         -                                 
CREATE_IN_PROGRESS                  AWS::IAM::Role                      ChatGPTRole                         Resource creation Initiated       
CREATE_COMPLETE                     AWS::IAM::ManagedPolicy             ChimePolicy                         -                                 
CREATE_COMPLETE                     AWS::IAM::Role                      BotRuntimeRole                      -                                 
CREATE_COMPLETE                     AWS::IAM::Role                      ChimeSMARole                        -                                 
CREATE_IN_PROGRESS                  AWS::IAM::Role                      TransferCallRole                    -                                 
CREATE_IN_PROGRESS                  AWS::IAM::Role                      TransferCallRole                    Resource creation Initiated       
CREATE_IN_PROGRESS                  AWS::Lex::Bot                       LexBotGPT                           -                                 
CREATE_IN_PROGRESS                  AWS::Lex::Bot                       LexBotGPT                           Resource creation Initiated       
CREATE_COMPLETE                     AWS::S3::Bucket                     RecordBucket                        -                                 
CREATE_COMPLETE                     AWS::S3::Bucket                     PromptBucket                        -                                 
CREATE_IN_PROGRESS                  AWS::S3::BucketPolicy               RecordBucketPolicy                  -                                 
CREATE_IN_PROGRESS                  AWS::IAM::Role                      PromptCreatorRole                   -                                 
CREATE_IN_PROGRESS                  AWS::IAM::Role                      PromptCopierRole                    -                                 
CREATE_IN_PROGRESS                  AWS::IAM::Role                      PromptCreatorRole                   Resource creation Initiated       
CREATE_IN_PROGRESS                  AWS::S3::BucketPolicy               PromptBucketPolicy                  -                                 
CREATE_IN_PROGRESS                  AWS::IAM::Role                      PromptCopierRole                    Resource creation Initiated       
CREATE_IN_PROGRESS                  AWS::S3::BucketPolicy               RecordBucketPolicy                  Resource creation Initiated       
CREATE_COMPLETE                     AWS::S3::BucketPolicy               RecordBucketPolicy                  -                                 
CREATE_IN_PROGRESS                  AWS::S3::BucketPolicy               PromptBucketPolicy                  Resource creation Initiated       
CREATE_COMPLETE                     AWS::S3::BucketPolicy               PromptBucketPolicy                  -                                 
CREATE_COMPLETE                     AWS::IAM::Role                      ChatGPTRole                         -                                 
CREATE_COMPLETE                     AWS::IAM::Role                      TransferCallRole                    -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Function               ChatGPT                             -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Function               ChatGPT                             Resource creation Initiated       
CREATE_IN_PROGRESS                  AWS::Lambda::Function               TransferCall                        -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Function               TransferCall                        Resource creation Initiated       
CREATE_COMPLETE                     AWS::IAM::Role                      PromptCreatorRole                   -                                 
CREATE_COMPLETE                     AWS::IAM::Role                      PromptCopierRole                    -                                 
CREATE_COMPLETE                     AWS::Lambda::Function               ChatGPT                             -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Function               PromptCopier                        -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Function               PromptCreator                       -                                 
CREATE_COMPLETE                     AWS::Lambda::Function               TransferCall                        -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Version                ChatGPTVersion3d508bab8c            -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Permission             LexToChatGPTPerm                    -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Function               PromptCopier                        Resource creation Initiated       
CREATE_IN_PROGRESS                  AWS::Lambda::Permission             LexToChatGPTPerm                    Resource creation Initiated       
CREATE_IN_PROGRESS                  AWS::Lambda::Function               PromptCreator                       Resource creation Initiated       
CREATE_IN_PROGRESS                  AWS::Lambda::Version                ChatGPTVersion3d508bab8c            Resource creation Initiated       
CREATE_IN_PROGRESS                  AWS::Connect::IntegrationAssociat   TransferCallConnectIntegration      -                                 
                                    ion                                                                                                       
CREATE_IN_PROGRESS                  AWS::Connect::IntegrationAssociat   TransferCallConnectIntegration      Resource creation Initiated       
                                    ion                                                                                                       
CREATE_COMPLETE                     AWS::Connect::IntegrationAssociat   TransferCallConnectIntegration      -                                 
                                    ion                                                                                                       
CREATE_COMPLETE                     AWS::Lex::Bot                       LexBotGPT                           -                                 
CREATE_COMPLETE                     AWS::Lambda::Function               PromptCopier                        -                                 
CREATE_COMPLETE                     AWS::Lambda::Function               PromptCreator                       -                                 
CREATE_IN_PROGRESS                  AWS::Lex::BotVersion                BotVersionGPT                       -                                 
CREATE_IN_PROGRESS                  Custom::PromptCopier                StaticPrompts                       -                                 
CREATE_IN_PROGRESS                  Custom::PromptCreator               TansferPromptEN                     -                                 
CREATE_IN_PROGRESS                  Custom::PromptCreator               TryAgainEN                          -                                 
CREATE_IN_PROGRESS                  Custom::PromptCreator               MainMenuEN                          -                                 
CREATE_IN_PROGRESS                  Custom::PromptCreator               TansferPromptES                     -                                 
CREATE_IN_PROGRESS                  Custom::PromptCreator               MainPromptWest                      -                                 
CREATE_IN_PROGRESS                  Custom::PromptCreator               TryAgainES                          -                                 
CREATE_IN_PROGRESS                  Custom::PromptCreator               MainMenuES                          -                                 
CREATE_IN_PROGRESS                  Custom::PromptCreator               GoodbyePromptEN                     -                                 
CREATE_IN_PROGRESS                  Custom::PromptCreator               MainPromptEast                      -                                 
CREATE_IN_PROGRESS                  Custom::PromptCreator               GoodbyePromptES                     -                                 
CREATE_IN_PROGRESS                  AWS::Lex::BotVersion                BotVersionGPT                       Resource creation Initiated       
CREATE_COMPLETE                     AWS::Lambda::Permission             LexToChatGPTPerm                    -                                 
CREATE_IN_PROGRESS                  Custom::PromptCreator               TryAgainEN                          Resource creation Initiated       
CREATE_IN_PROGRESS                  Custom::PromptCreator               GoodbyePromptEN                     Resource creation Initiated       
CREATE_COMPLETE                     Custom::PromptCreator               TryAgainEN                          -                                 
CREATE_COMPLETE                     Custom::PromptCreator               GoodbyePromptEN                     -                                 
CREATE_IN_PROGRESS                  Custom::PromptCreator               TansferPromptEN                     Resource creation Initiated       
CREATE_IN_PROGRESS                  Custom::PromptCreator               TansferPromptES                     Resource creation Initiated       
CREATE_IN_PROGRESS                  Custom::PromptCreator               MainPromptWest                      Resource creation Initiated       
CREATE_IN_PROGRESS                  Custom::PromptCreator               MainMenuEN                          Resource creation Initiated       
CREATE_COMPLETE                     Custom::PromptCreator               TansferPromptEN                     -                                 
CREATE_COMPLETE                     Custom::PromptCreator               TansferPromptES                     -                                 
CREATE_COMPLETE                     Custom::PromptCreator               MainPromptWest                      -                                 
CREATE_IN_PROGRESS                  Custom::PromptCreator               MainMenuES                          Resource creation Initiated       
CREATE_IN_PROGRESS                  Custom::PromptCreator               TryAgainES                          Resource creation Initiated       
CREATE_COMPLETE                     Custom::PromptCreator               MainMenuEN                          -                                 
CREATE_IN_PROGRESS                  Custom::PromptCreator               MainPromptEast                      Resource creation Initiated       
CREATE_IN_PROGRESS                  Custom::PromptCopier                StaticPrompts                       Resource creation Initiated       
CREATE_IN_PROGRESS                  Custom::PromptCreator               GoodbyePromptES                     Resource creation Initiated       
CREATE_COMPLETE                     Custom::PromptCreator               MainMenuES                          -                                 
CREATE_COMPLETE                     Custom::PromptCreator               TryAgainES                          -                                 
CREATE_COMPLETE                     Custom::PromptCreator               MainPromptEast                      -                                 
CREATE_COMPLETE                     Custom::PromptCopier                StaticPrompts                       -                                 
CREATE_COMPLETE                     Custom::PromptCreator               GoodbyePromptES                     -                                 
CREATE_COMPLETE                     AWS::Lex::BotVersion                BotVersionGPT                       -                                 
CREATE_COMPLETE                     AWS::Lambda::Version                ChatGPTVersion3d508bab8c            -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Alias                  ChatGPTAliasSNAPSTART               -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Alias                  ChatGPTAliasSNAPSTART               Resource creation Initiated       
CREATE_COMPLETE                     AWS::Lambda::Alias                  ChatGPTAliasSNAPSTART               -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Permission             LexToChatGPTSnapPerm                -                                 
CREATE_IN_PROGRESS                  AWS::Lex::BotAlias                  BotAliasGPT                         -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Permission             LexToChatGPTSnapPerm                Resource creation Initiated       
CREATE_IN_PROGRESS                  AWS::Lex::BotAlias                  BotAliasGPT                         Resource creation Initiated       
CREATE_COMPLETE                     AWS::Lex::BotAlias                  BotAliasGPT                         -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Function               ChimeSMA                            -                                 
CREATE_IN_PROGRESS                  AWS::Lex::ResourcePolicy            ChimeCallLexGPT                     -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Function               ChimeSMA                            Resource creation Initiated       
CREATE_IN_PROGRESS                  AWS::Lex::ResourcePolicy            ChimeCallLexGPT                     Resource creation Initiated       
CREATE_COMPLETE                     AWS::Lex::ResourcePolicy            ChimeCallLexGPT                     -                                 
CREATE_COMPLETE                     AWS::Lambda::Permission             LexToChatGPTSnapPerm                -                                 
CREATE_COMPLETE                     AWS::Lambda::Function               ChimeSMA                            -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Permission             ChimeSMAPerm                        -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Permission             ChimeSMAPerm                        Resource creation Initiated       
CREATE_COMPLETE                     AWS::Lambda::Permission             ChimeSMAPerm                        -                                 
CREATE_COMPLETE                     AWS::CloudFormation::Stack          chime-voicesdk-sma                  -                                 
---------------------------------------------------------------------------------------------------------------------------------------------


Successfully created/updated stack - chime-voicesdk-sma in us-west-2


```
