# Amazon Chime SDK SIP Media Application(SMA) Java Event Library

## JSON Java Events

AWS Lambda is excellent at handling events in JSON. AWS provides the [Lambda Java Events](https://github.com/aws/aws-lambda-java-libs/tree/main/aws-lambda-java-events) library, which handles most of the services that directly integrate with Lambda and provides a full Java Object model for the requests and responses. However, the Chime SMA events are not included in this package. This library follows a similar approach and is used as follows:

- You define your Lambda to implement [RequestHandler](https://github.com/aws/aws-lambda-java-libs/blob/main/aws-lambda-java-core/src/main/java/com/amazonaws/services/lambda/runtime/RequestHandler.java)<[SMARequest](/ChimeSMAEvent/src/main/java/cloud/cleo/chimesma/model/SMARequest.java), [SMAResponse](/ChimeSMAEvent/src/main/java/cloud/cleo/chimesma/model/SMAResponse.java)>.
- Process the incoming request and respond as necessary. Refer to the [Helloworld.java](/Examples/src/main/java/cloud/cleo/chimesma/examples/response/HelloWorld.java) for an example.
- Note that you are responsible for handling the SMA state machine, and this quickly becomes unmanageable as the complexity of your application increases.
- Use this low-level approach when you need to control every aspect of the state machine or for simple applications.


## Building the Library

At some point the the library components will be published to the central repo, but in the meantime you must build and intall the library locally.

```bash
echo "Clone the repo"
git clone https://github.com/docwho2/java-chime-voicesdk-sma.git

echo "Change directory into the cloned repo"
cd java-chime-voicesdk-sma

echo "Retrieve the V4 Events sub-module"
git submodule update --init

echo "Build and Install all artifacts"
mvn install
```

After running the above you should see:

```bash
[INFO] Reactor Summary:
[INFO] 
[INFO] AWS Lambda Java Runtime Serialization 2.0.0 ........ SUCCESS [  7.869 s]
[INFO] AWS Lambda Java Events Library 4.0.0 ............... SUCCESS [  6.248 s]
[INFO] Chime SMA Parent POM 1.0 ........................... SUCCESS [  0.002 s]
[INFO] Chime SDK SMA Event Library 1.0 .................... SUCCESS [  0.790 s]
[INFO] Chime SDK SMA Flow Library 1.0 ..................... SUCCESS [  1.227 s]
[INFO] Chime Polly Prompt Generator 1.0 ................... SUCCESS [  5.528 s]
[INFO] Chime Lex ChatGPT Lambda 1.0 ....................... SUCCESS [  4.142 s]
[INFO] Chime SDK SMA Examples 1.0 ......................... SUCCESS [  1.250 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  27.848 s
[INFO] Finished at: 2023-07-15T05:18:16-05:00
[INFO] ------------------------------------------------------------------------
```

## Including the library in your project

### Maven
```xml
   <dependency>
     <groupId>cloud.cleo.chimesma</groupId>
     <artifactId>sma-lambda-event-lib</artifactId>
     <version>1.0</version>
   </dependency>
```


### Environment Variables

Typically in your CloudFormation or whatever you use to deploy your Lambda, it might be easier to set these ENV variables.

- PROMPT_BUCKET - When using any of the Play Actions like [PlayAudio](/ChimeSMAEvent/src/main/java/cloud/cleo/chimesma/model/ResponsePlayAudio.java) you can omit the S3 BucketName if this variable is set.
- RECORD_BUCKET - When using any of the Record Actions like [RecordAudio](/ChimeSMAEvent/src/main/java/cloud/cleo/chimesma/model/ResponseRecordAudio.java) you can omit the S3 BucketName if this variable is set.
- BOT_ALIAS_ARN - When using the [StartBotConversion](/ChimeSMAEvent/src/main/java/cloud/cleo/chimesma/model/ResponseStartBotConversation.java) Action you can omit the BotAliasArn if this variable is set.

