# Amazon Chime SDK SIP Media Application(SMA) Java Library


The library provides two approaches: a JSON event mapping model and, on top of that, a flow-based library that allows programmers to develop call flows without having to deal with event handling and flow optimization. The latter enables developers to quickly build applications with minimal coding.

### JSON Java Events

AWS Lambda is excellent at handling events in JSON. AWS provides the [Lambda Java Events](https://github.com/aws/aws-lambda-java-libs/tree/main/aws-lambda-java-events) library, which handles most of the services that directly integrate with Lambda and provides a full Java Object model for the requests and responses. However, the Chime SMA events are not included in this package. This library follows a similar approach and is used as follows:

- You define your Lambda to implement [RequestHandler](https://github.com/aws/aws-lambda-java-libs/blob/main/aws-lambda-java-core/src/main/java/com/amazonaws/services/lambda/runtime/RequestHandler.java)<[SMARequest](/ChimeSMA/src/main/java/cloud/cleo/chimesma/model/SMARequest.java), [SMAResponse](/ChimeSMA/src/main/java/cloud/cleo/chimesma/model/SMAResponse.java)>.
- Process the incoming request and respond as necessary. Refer to [Helloworld.java](/Examples/src/main/java/cloud/cleo/chimesma/examples/response/HelloWorld.java) for an example.
- Note that you are responsible for handling the SMA state machine, and this quickly becomes unmanageable as the complexity of your application increases.
- Use this low-level approach when you need to control every aspect of the state machine.

### Java Action Flow Model

Building upon the above, the "Action Flow Model" maps each of the [supported actions for the PSTN Audio service](https://docs.aws.amazon.com/chime-sdk/latest/dg/specify-actions.html) to Java Objects that you can dynamically connect to each other to create flows. These objects are easily extensible, allowing for the creation of complex interactions and routing. This part of the library provides:

- A flow-based approach that makes developing applications easier to understand compared to an event-driven model.
  - The flow is built statically in memory during initialization (SNAP Start Init), enabling very low latency responses.
- Strong Java typing and a builder approach for creating action objects.
- Sensible defaults for most of the action parameters, which can be set or passed into the Lambda (e.g., BOT_ALIAS_ARN to start bot conversations).
- Derived values whenever possible to reduce lines of code.
  - Speak actions require you to set SSML or Text for operations. The flow action takes only a text parameter and determines and sets the correct value when sending back the JSON.
- Response optimization by sending multiple actions at once (SMA supports up to 10).
  - Each action is evaluated to determine whether it can be chained with another before sending the action list.
  - Example: Pause -> Speak -> Pause -> StartRecording -> SpeakAndGetDigits, when sent one by one, would require 4 Lambda calls without optimization.
  - Actions like SpeakAndGetDigits require a result before proceeding and cannot be chained with another action.
- Java Locale support across all relevant actions to easily build multilingual interactions (Prompts, Speak, and Bots).
- Easy extension of existing actions. See [CallAndBridgeActionTBTDiversion.java](/Examples/src/main/java/cloud/cleo/chimesma/actions/CallAndBridgeActionTBTDiversion.java), which extends the standard [CallAndBridge](/ChimeSMA/src/main/java/cloud/cleo/chimesma/actions/CallAndBridgeAction.java) action to write call information to a DynamoDB table that can be used in a Connect flow to implement take-back and transfer. See the use case later in this document.

To use the Flow Model:

- Simply create a Java Object that inherits from [AbstractFlow](/ChimeSMA/src/main/java/cloud/cleo/chimesma/actions/AbstractFlow.java).
- Implement the `getInitialAction()` method, which returns the start of the flow.
- Refer to the flow-based [HelloWorld.java](/Examples/src/main/java/cloud/cleo/chimesma/examples/actions/HelloWorld.java) example.
