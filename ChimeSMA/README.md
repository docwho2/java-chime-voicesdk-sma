# Amazon Chime SDK SIP Media Application(SMA) Java Library


The library provides two approaches: a JSON event mapping model and, on top of that, a flow-based library that allows programmers to develop call flows without having to deal with event handling and flow optimization. The latter enables developers to quickly build applications with minimal coding.

## JSON Java Events

AWS Lambda is excellent at handling events in JSON. AWS provides the [Lambda Java Events](https://github.com/aws/aws-lambda-java-libs/tree/main/aws-lambda-java-events) library, which handles most of the services that directly integrate with Lambda and provides a full Java Object model for the requests and responses. However, the Chime SMA events are not included in this package. This library follows a similar approach and is used as follows:

- You define your Lambda to implement [RequestHandler](https://github.com/aws/aws-lambda-java-libs/blob/main/aws-lambda-java-core/src/main/java/com/amazonaws/services/lambda/runtime/RequestHandler.java)<[SMARequest](/ChimeSMA/src/main/java/cloud/cleo/chimesma/model/SMARequest.java), [SMAResponse](/ChimeSMA/src/main/java/cloud/cleo/chimesma/model/SMAResponse.java)>.
- Process the incoming request and respond as necessary. Refer to [Helloworld.java](/Examples/src/main/java/cloud/cleo/chimesma/examples/response/HelloWorld.java) for an example.
- Note that you are responsible for handling the SMA state machine, and this quickly becomes unmanageable as the complexity of your application increases.
- Use this low-level approach when you need to control every aspect of the state machine.

## Java Action Flow Model

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


## Building the Library

At some point the the library components will be published to the central repo, but in the meantime you must build and intall the library locally.

```bash
echo "Clone the repo"
git clone https://github.com/docwho2/java-chime-voicesdk-sma.git

echo "Chage directory into the cloned repo"
cd java-chime-voicesdk-sma

echo "Retrieve the V4 Events sub-module"
git submodule update --init

echo "Build and Install all artifacts"
mvn install
```

After running the above you should see:

```bash
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary:
[INFO] 
[INFO] Chime SMA Parent POM 1.0 ........................... SUCCESS [  0.085 s]
[INFO] Chime Polly Prompt Generator 1.0 ................... SUCCESS [  2.609 s]
[INFO] AWS Lambda Java Runtime Serialization 2.0.0 ........ SUCCESS [  1.122 s]
[INFO] Chime SDK SMA Library 1.0 .......................... SUCCESS [  1.179 s]
[INFO] AWS Lambda Java Events Library 4.0.0 ............... SUCCESS [  3.061 s]
[INFO] Chime Lex ChatGPT Lambda 1.0 ....................... SUCCESS [  1.759 s]
[INFO] Chime SDK Examples 1.0 ............................. SUCCESS [  1.230 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  11.129 s
[INFO] Finished at: 2023-07-05T07:01:00-05:00
[INFO] ------------------------------------------------------------------------
```

## Including the library in your project

### Maven
```xml
   <dependency>
     <groupId>cloud.cleo.chimesma</groupId>
     <artifactId>sma-lambda-lib</artifactId>
     <version>1.0</version>
   </dependency>
```

## Using the Library

See [examples](/Examples/src/main/java/cloud/cleo/chimesma/examples/actions/ExampleActions.java) of creating Actions (Note: for brevity, the actions 
don't end with ".build()").

For a full example refer to the [Demo Application](/Examples/src/main/java/cloud/cleo/chimesma/examples/actions/ExampleFlow.java).

### Environment Variables

TODO: document the ENV vars used in the Library

## Building the Hello World App

A simple SAM [template](/Examples/template.yaml) is provided to get started which deploys the Hello World Lambda.

```bash
echo "Chage directory into the Examples"
cd java-chime-voicesdk-sma

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
