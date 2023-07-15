# Amazon Chime SDK SIP Media Application(SMA) Java Flow Library


## Java Action Flow Model

Building upon the [Event Libray](/ChimeSMAEvent), the "Action Flow Model" maps each of the [supported actions for the PSTN Audio service](https://docs.aws.amazon.com/chime-sdk/latest/dg/specify-actions.html) to Java Objects that you can dynamically connect to each other to create flows. These objects are easily extensible, allowing for the creation of complex interactions and routing. This part of the library provides:

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
- Easy extension of existing actions. See [CallAndBridgeActionTBTDiversion.java](/Examples/src/main/java/cloud/cleo/chimesma/actions/CallAndBridgeActionTBTDiversion.java), which extends the standard [CallAndBridge](/ChimeSMAFlow/src/main/java/cloud/cleo/chimesma/actions/CallAndBridgeAction.java) action to write call information to a DynamoDB table that can be used in a Connect flow to implement take-back and transfer. See the use case later in this document.

To use the Flow Model:

- Simply create a Java Object that inherits from [AbstractFlow](/ChimeSMAFlow/src/main/java/cloud/cleo/chimesma/actions/AbstractFlow.java).
- Implement the `getInitialAction()` method, which returns the start of the flow.
- Refer to the flow-based [HelloWorld.java](/Examples/src/main/java/cloud/cleo/chimesma/examples/actions/HelloWorld.java) example.


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
     <artifactId>sma-lambda-flow-lib</artifactId>
     <version>1.0</version>
   </dependency>
```

## Using the Library

See [examples](/Examples/src/main/java/cloud/cleo/chimesma/examples/actions/ExampleActions.java) of creating Actions (Note: for brevity, the actions 
don't end with ".build()").

For a full example refer to the [Demo Application](/Examples/src/main/java/cloud/cleo/chimesma/examples/actions/ExampleFlow.java).

### Environment Variables

Typically in your CloudFormation or whatever you use to deploy your Lambda, it might be easier to set these ENV variables.

- PROMPT_BUCKET - When using any of the Play Actions like [PlayAudio](/ChimeSMAFlow/src/main/java/cloud/cleo/chimesma/actions/PlayAudioAction.java) you can omit the S3 BucketName if this variable is set.
- RECORD_BUCKET - When using any of the Record Actions like [RecordAudio](/ChimeSMAFlow/src/main/java/cloud/cleo/chimesma/actions/RecordAudioAction.java) you can omit the S3 BucketName if this variable is set.
- BOT_ALIAS_ARN - When using the [StartBotConversion](/ChimeSMAFlow/src/main/java/cloud/cleo/chimesma/actions/StartBotConversationAction.java) Action you can omit the BotAliasArn if this variable is set.
- LANGUAGE_VOICE_MAP - When using any Speak actions, you can provide a JSON array that indicates which VoiceId you want to use for each locale making it easier to support multi-lingual apps.

An example LANGUAGE_VOICE_MAP:
```json
[
  { "Locale" : "en-US" , "VoiceId" : "Joanna" },
  { "Locale" : "es-US" , "VoiceId" : "Lupe" }
]
```

Constructing the same JSON in CloudFormation for a Lambda in the Environment section:

```yaml
    Environment: 
        Variables:
          PROMPT_BUCKET: !Ref PromptBucket
          RECORD_BUCKET: !Ref RecordBucket
          BOT_ALIAS_ARN: !GetAtt BotAliasGPT.Arn
          LANGUAGE_VOICE_MAP:
              Fn::ToJsonString:  # Note: this requires AWS::LanguageExtensions in the Transform section
                  - Locale: en-US
                    VoiceId: Joanna
                  - Locale: es-US
                    VoiceId: Lupe
```

See the main the project [template.yaml](https://github.com/docwho2/java-chime-voicesdk-sma/blob/main/template.yaml#L287) for an example of creating 
resources like S3 Buckets and Bots and then passing these in via ENV vars the SMA Lambda.  If you have several Bots for example, you might to create 
you own ENV vars in addition to above to support N number of Bots.

## Building the Hello World App

A simple SAM [template](/Examples/template.yaml) is provided to get started which deploys the Hello World Lambda.

```bash
echo "Chage directory into the Examples"
cd Examples

echo "Build the Example Hello World"
sam build

echo "Deploy the Lambda"
sam deploy
```

The output will look like:

```
$ sam build
Building codeuri: /java-chime-voicesdk-sma/Examples runtime: java11 metadata: {} architecture: x86_64 functions: ChimeSMA    
Running JavaMavenWorkflow:CopySource                                                                                                            
Running JavaMavenWorkflow:MavenBuild                                                                                                            
Running JavaMavenWorkflow:MavenCopyDependency                                                                                                   
Running JavaMavenWorkflow:MavenCopyArtifacts                                                                                                    

Build Succeeded

Built Artifacts  : .aws-sam/build
Built Template   : .aws-sam/build/template.yaml

Commands you can use next
=========================
[*] Validate SAM template: sam validate
[*] Invoke Function: sam local invoke
[*] Test Function in the Cloud: sam sync --stack-name {{stack-name}} --watch
[*] Deploy: sam deploy --guided



$ sam deploy

		Managed S3 bucket: aws-sam-cli-managed-default-samclisourcebucket-13mtys65mpu
		A different default S3 bucket can be set in samconfig.toml
		Or by specifying --s3-bucket explicitly.
	Uploading to chime-voicesdk-hello-world/a9276c89d44f718f145e42c840061669  14703735 / 14703735  (100.00%)

	Deploying with following values
	===============================
	Stack name                   : chime-voicesdk-hello-world
	Region                       : us-east-1
	Confirm changeset            : True
	Disable rollback             : False
	Deployment s3 bucket         : aws-sam-cli-managed-default-samclisourcebucket-13mty565mpu
	Capabilities                 : ["CAPABILITY_IAM"]
	Parameter overrides          : {"VOICEIDEN": "Joanna", "VOICEIDES": "Lupe"}
	Signing Profiles             : {}

Initiating deployment
=====================

	Uploading to chime-voicesdk-hello-world/42afe9f6b20620eb5d3c32218161cb6e.template  2350 / 2350  (100.00%)


Waiting for changeset to be created..

CloudFormation stack changeset
---------------------------------------------------------------------------------------------------------------------------------------------
Operation                           LogicalResourceId                   ResourceType                        Replacement                       
---------------------------------------------------------------------------------------------------------------------------------------------
+ Add                               ChimeSMAAliasSNAPSTART              AWS::Lambda::Alias                  N/A                               
+ Add                               ChimeSMAPerm                        AWS::Lambda::Permission             N/A                               
+ Add                               ChimeSMARole                        AWS::IAM::Role                      N/A                               
+ Add                               ChimeSMASnapPerm                    AWS::Lambda::Permission             N/A                               
+ Add                               ChimeSMAVersionf60f2174b4           AWS::Lambda::Version                N/A                               
+ Add                               ChimeSMA                            AWS::Lambda::Function               N/A                               
---------------------------------------------------------------------------------------------------------------------------------------------


Changeset created successfully. arn:aws:cloudformation:us-east-1::changeSet/samcli-deploy1688562362/916a367d-6f3a-435d-a54e-15a334c92e32


Previewing CloudFormation changeset before deployment
======================================================
Deploy this changeset? [y/N]: y

2023-07-05 08:06:28 - Waiting for stack create/update to complete

CloudFormation events from stack operations (refresh every 5.0 seconds)
---------------------------------------------------------------------------------------------------------------------------------------------
ResourceStatus                      ResourceType                        LogicalResourceId                   ResourceStatusReason              
---------------------------------------------------------------------------------------------------------------------------------------------
CREATE_IN_PROGRESS                  AWS::IAM::Role                      ChimeSMARole                        -                                 
CREATE_IN_PROGRESS                  AWS::IAM::Role                      ChimeSMARole                        Resource creation Initiated       
CREATE_COMPLETE                     AWS::IAM::Role                      ChimeSMARole                        -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Function               ChimeSMA                            -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Function               ChimeSMA                            Resource creation Initiated       
CREATE_COMPLETE                     AWS::Lambda::Function               ChimeSMA                            -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Permission             ChimeSMAPerm                        -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Version                ChimeSMAVersionf60f2174b4           -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Permission             ChimeSMAPerm                        Resource creation Initiated       
CREATE_IN_PROGRESS                  AWS::Lambda::Version                ChimeSMAVersionf60f2174b4           Resource creation Initiated       
CREATE_COMPLETE                     AWS::Lambda::Permission             ChimeSMAPerm                        -                                 
CREATE_COMPLETE                     AWS::Lambda::Version                ChimeSMAVersionf60f2174b4           -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Alias                  ChimeSMAAliasSNAPSTART              -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Alias                  ChimeSMAAliasSNAPSTART              Resource creation Initiated       
CREATE_COMPLETE                     AWS::Lambda::Alias                  ChimeSMAAliasSNAPSTART              -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Permission             ChimeSMASnapPerm                    -                                 
CREATE_IN_PROGRESS                  AWS::Lambda::Permission             ChimeSMASnapPerm                    Resource creation Initiated       
CREATE_COMPLETE                     AWS::Lambda::Permission             ChimeSMASnapPerm                    -                                 
CREATE_COMPLETE                     AWS::CloudFormation::Stack          chime-voicesdk-hello-world          -                                 
---------------------------------------------------------------------------------------------------------------------------------------------

CloudFormation outputs from deployed stack
---------------------------------------------------------------------------------------------------------------------------------------------
Outputs                                                                                                                                     
---------------------------------------------------------------------------------------------------------------------------------------------
Key                 FunctionARN                                                                                                             
Description         Set this Lambda ARN on your SIP Media Application                                                                       
Value               arn:aws:lambda:us-east-1::function:chime-voicesdk-hello-world-HelloWorld:SNAPSTART                          
---------------------------------------------------------------------------------------------------------------------------------------------


Successfully created/updated stack - chime-voicesdk-hello-world in us-east-1
```
