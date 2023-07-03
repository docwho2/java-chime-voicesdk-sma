/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.chimesma;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.lambda.powertools.cloudformation.AbstractCustomResourceHandler;
import software.amazon.lambda.powertools.cloudformation.Response;

/**
 *  Copy all prompts in the repo in the resources/prompts directory to S3 Prompt Bucket
 * so they can be Played by Chime
 * @author sjensen
 */
public class PromptCopier extends AbstractCustomResourceHandler {

    // Initialize the Log4j logger.
    Logger log = LogManager.getLogger();

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

            // Process all the prompts
            for (var path : getPromptList()) {
                log.debug("Copying Prompt [" + path + "] to S3");
                final var por = PutObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        // Chime requires audio/wav, but a default copy would set audio/x-wav 
                        .contentType("audio/wav") 
                        .key(path.getFileName().toString())
                        .build();

                // Push the wav file into the prompt bucket
                s3.putObject(por, RequestBody.fromFile(path));
            }

        } catch (Exception e) {
            log.error("Could Not copy prompts", e);
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
    protected Response delete(CloudFormationCustomResourceEvent cfcre, Context cntxt) {
        try {

            // Process all the prompts
            for (var path : getPromptList()) {
                log.debug("Deleting Prompt [" + path + "] from S3");

                final var dor = DeleteObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(path.getFileName().toString())
                        .build();

                s3.deleteObject(dor);
            }

        } catch (Exception e) {
            log.error("Could Not delete the static prompts", e);
        }
        return Response.builder()
                .value(cfcre.getResourceProperties())
                .build();
    }

    /**
     *
     * List of all files in the prompt directory
     *
     */
    private List<Path> getPromptList() throws IOException {

        // get the task root which is where all the resources will be (sox binary)
        final var task_root = System.getenv("LAMBDA_TASK_ROOT");

        // Path to where the static prompts are
        final var prompt_path = Path.of(task_root, "prompts");

        return Files.list(prompt_path)
                .filter(Files::isRegularFile)
                .filter(f -> f.getFileName().toString().toLowerCase().endsWith(".wav"))
                .collect(Collectors.toList());
    }

    /**
     * We don't do anything on stack updates, just return null
     *
     * @param cfcre
     * @param cntxt
     * @return
     */
    @Override
    protected Response update(CloudFormationCustomResourceEvent cfcre, Context cntxt) {
        log.debug("Received UPDATE Event from Cloudformation", cfcre);
        return null;
    }

}
