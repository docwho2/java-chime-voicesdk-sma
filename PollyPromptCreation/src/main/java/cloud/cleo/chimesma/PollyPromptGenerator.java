/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.Engine;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.model.TextType;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.lambda.powertools.cloudformation.AbstractCustomResourceHandler;
import software.amazon.lambda.powertools.cloudformation.Response;

/**
 * Custom Resource to generate prompts from CloudFormation.  Prompts can be created,
 * updated, and deleted as template changes.  Ultimately, anything created is deleted
 * so prompt buckets can be deleted as well.
 * 
 * @author sjensen
 */
public class PollyPromptGenerator extends AbstractCustomResourceHandler {

    // Initialize the Log4j logger.
    Logger log = LogManager.getLogger(PollyPromptGenerator.class);

    private final static PollyClient polly = PollyClient.builder()
            .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .httpClient(UrlConnectionHttpClient.builder().build())
            .build();

    private final static S3Client s3 = S3Client.builder()
            .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .httpClient(UrlConnectionHttpClient.builder().build())
            .build();

    private final static String BUCKET_NAME = System.getenv("PROMPT_BUCKET");

    @Override
    protected Response create(CloudFormationCustomResourceEvent cfcre, Context cntxt) {
        log.debug("Received CREATE Event from Cloudformation");
        log.debug(cfcre);

        try {
            final var name = cfcre.getResourceProperties().get("PromptName").toString();
            final var text = cfcre.getResourceProperties().get("PromptText").toString();
            final var voice_id = cfcre.getResourceProperties().get("VoiceId").toString();

            createPrompt(name, text, voice_id);

        } catch (Exception e) {
            log.error("Could Not create the prompt", e);
        }
        return Response.builder()
                .value(cfcre.getResourceProperties())
                .build();
    }

    /**
     * Update the Prompt if anything has changed
     *
     * @param cfcre
     * @param cntxt
     * @return
     */
    @Override
    protected Response update(final CloudFormationCustomResourceEvent cfcre, final Context cntxt) {
        log.debug("Received UPDATE Event from Cloudformation", cfcre);

        // Old Values
        final var name_old = cfcre.getOldResourceProperties().get("PromptName").toString();
        final var text_old = cfcre.getOldResourceProperties().get("PromptText").toString();
        final var voice_id_old = cfcre.getOldResourceProperties().get("VoiceId").toString();

        // New Values
        final var name = cfcre.getResourceProperties().get("PromptName").toString();
        final var text = cfcre.getResourceProperties().get("PromptText").toString();
        final var voice_id = cfcre.getResourceProperties().get("VoiceId").toString();

        try {
            if (!Objects.equals(name, name_old)) {
                // Since the name has changed, requires delete of object and re-creaate
                log.debug("The filename has changed from [" + name_old + "] to [" + name + "]");

                // Delete the old file
                deleteS3Object(name_old);
                // Create new based on incoming values
                createPrompt(name, text, voice_id);
                
            } else if ( ( ! Objects.equals(text, text_old) ) || (! Objects.equals(voice_id, voice_id_old) ) ) {
                log.debug("The text has changed from [" + text_old + "] to [" + text + "]");
                log.debug("The voice has changed from [" + voice_id_old+ "] to [" + voice_id + "]");
                
                // Just re-creaate the prompt
                createPrompt(name, text, voice_id);
            } else {
                log.debug("No Changes were detected in the old vs new values, thus doing nothing !");
            }

        } catch (Exception e) {
            log.error("Could Not update the prompt", e);
        }

        return Response.builder()
                .value(cfcre.getResourceProperties())
                .build();
    }

    /**
     * Delete the prompts we created so the bucket can be deleted
     *
     * @param cfcre
     * @param cntxt
     * @return
     */
    @Override
    protected Response delete(final CloudFormationCustomResourceEvent cfcre, final Context cntxt) {
        try {
            final var name = cfcre.getResourceProperties().get("PromptName").toString();
            log.debug("Deleting Promp " + name);
            deleteS3Object(name);
        } catch (Exception e) {
            log.error("Could Not delete the prompt", e);
        }
        return Response.builder()
                .value(cfcre.getResourceProperties())
                .build();
    }

    /**
     * Create Prompt and Write the file to the S3 bucket
     *
     * @param name
     * @param text
     * @param voice_id
     * @throws IOException
     * @throws InterruptedException
     */
    private void createPrompt(final String name, final String text, final String voice_id) throws IOException, InterruptedException {
        final var ssr = SynthesizeSpeechRequest.builder()
                .engine(text.toLowerCase().contains("<speak>") ? Engine.STANDARD : Engine.NEURAL)
                .voiceId(voice_id)
                .sampleRate("8000")
                .outputFormat(OutputFormat.PCM)
                .textType(text.toLowerCase().contains("<speak>") ? TextType.SSML : TextType.TEXT)
                .text(text).build();

        // get the task root which is where all the resources will be (sox binary)
        final var task_root = System.getenv("LAMBDA_TASK_ROOT");

        // Sox binary is in the resource folder
        final var soxBinary = Path.of(task_root, "sox");
        log.debug("LD_LIBRARY_PATH=" + System.getenv("LD_LIBRARY_PATH"));

        // Name of temp for input to sox
        final var pollyFile = Path.of("/tmp", "polly_audio.pcm");

        // Name of temp for outout of sox
        final var wavFile = Path.of("/tmp", name);

        // Take the Polly output and write to temp file
        Files.copy(polly.synthesizeSpeech(ssr), pollyFile, StandardCopyOption.REPLACE_EXISTING);

        // Call sox to convert PCM file to WAV for Chime SDK Playback
        // https://docs.aws.amazon.com/connect/latest/adminguide/setup-prompts-s3.html
        final var command = String.format("%s -t raw -r 8000 -e signed -b 16 -c 1 %s -r 8000 -c 1 %s", soxBinary, pollyFile, wavFile);
        log.debug("Executing: " + command);
        final var process = Runtime.getRuntime().exec(command);

        final var inStream = new StreamGobbler(process.getInputStream(), log::debug);
        Executors.newSingleThreadExecutor().submit(inStream);

        final var errorStream = new StreamGobbler(process.getErrorStream(), log::debug);
        Executors.newSingleThreadExecutor().submit(errorStream);

        log.debug(" Process exited with " + process.waitFor());

        // Push the final wav file into the prompt bucket
        s3.putObject(PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                // Chime needs this to be set exactly to the below
                .contentType("audio/wav")
                .key(name)
                .build(),
                RequestBody.fromFile(wavFile)
        );
    }

    /**
     * Delete object in the prompt bucket
     *
     * @param name
     */
    private void deleteS3Object(final String name) {
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(name)
                .build()
        );
    }

    private static class StreamGobbler implements Runnable {

        private final InputStream inputStream;
        private final Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }
}
