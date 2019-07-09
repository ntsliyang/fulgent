import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.internal.Response;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.glacier.GlacierClient;
import software.amazon.awssdk.services.glacier.model.*;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import software.amazon.awssdk.services.sqs.model.AddPermissionRequest;
import software.amazon.awssdk.services.sqs.model.AddPermissionResponse;
import software.amazon.awssdk.utils.BinaryUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class ArchiveTransferManagerImpl implements ArchiveTransferManager {

    private static GlacierClient glacierClient;

    private static SqsClient sqsClient;

    private static SnsClient snsClient;

    private static AwsCredentialsProvider credentialsProvider;

    private static ClientOverrideConfiguration clientOverrideConfiguration;

    /** Threshold, in bytes, for when to use the multipart upload operations */
    private static final long MULTIPART_UPLOAD_SIZE_THRESHOLD = 1024L * 1024L * 100;

    /** The default chunk size, in bytes, when downloading in multiple chunks using range retrieval. */
    private static final long DEFAULT_DOWNLOAD_CHUNK_SIZE = 1024L * 1024 * 128;

    /** The minimum part size, in bytes, for a Glacier multipart upload. */
    private static final long MINIMUM_PART_SIZE = 1024L * 1024;

    /** The maximum part size, in bytes, for a Glacier multipart upload. */
    private static final long MAXIMUM_UPLOAD_PART_SIZE = 1024L * 1024 * 1024 * 4;

    private static long sleepTime = 600;

    private static String sqsQueueARN;
    private static String sqsQueueURL;
    private static String snsTopicARN;
    private static String snsSubscriptionARN;

    public ArchiveTransferManager.Builder Builder() {
        return new ArchiveTransferManagerBuilderImpl();
    }

    public GlacierClient getGlacierClient() {
        return glacierClient;
    }

    @Override
    public String upload(String accountId, String vaultName, String archiveDescription, String fileName) throws IOException {
        // return archiveId of uploaded file
        File file = new File(fileName);

        if (!file.exists()) {
            System.out.println("file does not exist");
            System.exit(1);
        }

        if (file.length() > MULTIPART_UPLOAD_SIZE_THRESHOLD) {
            CompleteMultipartUploadResponse comResponse = uploadInMultipleParts(accountId, vaultName,
                    archiveDescription, file);
            return comResponse.archiveId();
        } else {
            return uploadInSinglePart(accountId, vaultName, archiveDescription, file).archiveId();
        }
    }

    private long calculatePartSize(long fileSize) {
        long partSize = MINIMUM_PART_SIZE;
        int approxNumParts = 1;
        while (partSize * approxNumParts < fileSize && partSize*2 <= MAXIMUM_UPLOAD_PART_SIZE) {
            partSize *= 2;
            approxNumParts *= 2;
        }
        return partSize;
    }

     private CompleteMultipartUploadResponse uploadInMultipleParts(String accountId, String vaultName, String archiveDescription, File file)
            throws IOException {
        final long partSize = calculatePartSize(file.length());
        String partSizeString = Long.toString(partSize);
        InitiateMultipartUploadResponse initiateResponse = glacierClient
                .initiateMultipartUpload(
                        InitiateMultipartUploadRequest.builder()
                                .accountId(accountId)
                                .archiveDescription(archiveDescription)
                                .vaultName(vaultName)
                                .partSize(partSizeString).build());
        String uploadId = initiateResponse.uploadId();

        int filePosition = 0;
        long currentPosition = 0;
        byte[] buffer = new byte[(int) partSize];
        List<byte[]> binaryChecksums = new LinkedList<byte[]>();

        FileInputStream fileToUpload = new FileInputStream(file);
        String contentRange;
        int read = 0;

        while (currentPosition < file.length()) {
            read = fileToUpload.read(buffer, filePosition, buffer.length);
            if (read == -1) {
                break;
            }
            byte[] bytesRead = Arrays.copyOf(buffer, read);

            contentRange = String.format("bytes %s-%s/*", currentPosition, currentPosition + read - 1);
            String checksum = TreeHashGenerator.calculateTreeHash(new ByteArrayInputStream(bytesRead));
            byte[] binaryChecksum = BinaryUtils.fromHex(checksum);
            binaryChecksums.add(binaryChecksum);
            System.out.println(contentRange);

            //Upload part.
            UploadMultipartPartRequest partRequest = UploadMultipartPartRequest.builder()
                    .vaultName(vaultName)
                    .checksum(checksum)
                    .range(contentRange)
                    .uploadId(uploadId).build();

            UploadMultipartPartResponse partResponse = glacierClient.uploadMultipartPart(partRequest, RequestBody.fromBytes(bytesRead));
            System.out.println("Part uploaded, checksum: " + partResponse.checksum());

            currentPosition = currentPosition + read;
        }
        fileToUpload.close();
        String checksum = TreeHashGenerator.calculateTreeHash(binaryChecksums);

        CompleteMultipartUploadRequest comRequest = CompleteMultipartUploadRequest.builder()
                .vaultName(vaultName)
                .uploadId(uploadId)
                .checksum(checksum)
                .archiveSize(String.valueOf(file.length())).build();

        CompleteMultipartUploadResponse comResponse = glacierClient.completeMultipartUpload(comRequest);
        return comResponse;
    }

    private UploadArchiveResponse uploadInSinglePart(String accountId, String vaultName, String archiveDescription, File file) {
        String checksum = TreeHashGenerator.calculateTreeHash(file);
        try {
            byte[] body = new byte[(int) file.length()];

            final UploadArchiveRequest req = UploadArchiveRequest.builder()
                    .accountId(accountId)
                    .archiveDescription(archiveDescription)
                    .vaultName(vaultName)
                    .checksum(checksum)
                    .contentLength(file.length()).build();
            UploadArchiveResponse uploadArchiveResponse = glacierClient.uploadArchive(req, RequestBody.fromBytes(body));
            return uploadArchiveResponse;
        }
        catch (Exception e) {
            System.err.println("Archive not uploaded.");
            System.err.println(e);
        }
        return null;
    }

    private void setupSQS(String snsTopicName, String sqsQueueName) {
        CreateQueueRequest request = CreateQueueRequest.builder().queueName(sqsQueueName).build();
        CreateQueueResponse response = sqsClient.createQueue(request);
        sqsQueueURL = response.queueUrl();

        GetQueueAttributesRequest qRequest = GetQueueAttributesRequest.builder()
                .queueUrl(sqsQueueURL)
                .attributeNames(QueueAttributeName.fromValue("QueueArn"))
                .build();

        GetQueueAttributesResponse qResponse = sqsClient.getQueueAttributes(qRequest);
        sqsQueueARN = qResponse.attributesAsStrings().get("QueueArn");

        IamClient iamClient = IamClient.builder().build();

        AddPermissionRequest aRequest = AddPermissionRequest.builder()
                .awsAccountIds(iamClient.getUser().user().userId())
                .label("sqsPolicy")
                .actions("SendMessage")
                .queueUrl(sqsQueueURL)
                .build();
        AddPermissionResponse aResponse = sqsClient.addPermission(aRequest);
        System.out.println("--------");
    }

    private void setupSNS(String snsTopicName) {
        CreateTopicRequest request = CreateTopicRequest.builder()
                .name(snsTopicName)
                .build();
        CreateTopicResponse response = snsClient.createTopic(request);
        snsTopicARN = response.topicArn();

        SubscribeRequest request2 = SubscribeRequest.builder()
                .topicArn(snsTopicARN)
                .endpoint(sqsQueueARN)
                .protocol("sqs")
                .build();
        SubscribeResponse response2 = snsClient.subscribe(request2);

        snsSubscriptionARN = response2.subscriptionArn();
    }

    private String initiateDownloadJobRequest(String vaultName, String archiveId) {
        InitiateJobRequest request = InitiateJobRequest.builder()
                .vaultName(vaultName)
                .jobParameters(JobParameters.builder()
                        .archiveId(archiveId)
                        .snsTopic(snsTopicARN)
                        .type("archive-retrieval")
                        .build())
                .build();
        InitiateJobResponse response = glacierClient.initiateJob(request);
        return response.jobId();
    }

    private Boolean waitForJobToComplete(String jobId, String sqsQueueUrl) throws InterruptedException, JsonParseException, IOException {

        Boolean messageFound = false;
        Boolean jobSuccessful = false;
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();

        while (!messageFound) {
            List<Message> msgs = sqsClient.receiveMessage(
                    ReceiveMessageRequest.builder()
                            .queueUrl(sqsQueueUrl)
                            .maxNumberOfMessages(10).build())
                            .messages();

            if (msgs.size() > 0) {
                for (Message m : msgs) {
                    JsonParser jpMessage = factory.createParser(m.body());
                    JsonNode jobMessageNode = mapper.readTree(jpMessage);
                    String jobMessage = jobMessageNode.get("Message").textValue();

                    JsonParser jpDesc = factory.createParser(jobMessage);
                    JsonNode jobDescNode = mapper.readTree(jpDesc);
                    String retrievedJobId = jobDescNode.get("JobId").textValue();
                    String statusCode = jobDescNode.get("StatusCode").textValue();
                    if (retrievedJobId.equals(jobId)) {
                        messageFound = true;
                        if (statusCode.equals("Succeeded")) {
                            jobSuccessful = true;
                        }
                    }
                }

            } else {
                Thread.sleep(sleepTime * 1000);
            }
        }
        return (messageFound && jobSuccessful);
    }

    private long waitForJobToCompleteInChunks(String jobId, String sqsQueueUrl) throws InterruptedException, JsonParseException, IOException {
        Boolean messageFound = false;
        Boolean jobSuccessful = false;
        long archiveSizeInBytes = -1;
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();

        while (!messageFound) {
            List<Message> msgs = sqsClient.receiveMessage(
                    ReceiveMessageRequest.builder()
                            .queueUrl(sqsQueueUrl)
                            .maxNumberOfMessages(10).build())
                    .messages();

            if (msgs.size() > 0) {
                for (Message m : msgs) {
                    JsonParser jpMessage = factory.createParser(m.body());
                    JsonNode jobMessageNode = mapper.readTree(jpMessage);
                    String jobMessage = jobMessageNode.get("Message").textValue();

                    JsonParser jpDesc = factory.createParser(jobMessage);
                    JsonNode jobDescNode = mapper.readTree(jpDesc);
                    String retrievedJobId = jobDescNode.get("JobId").textValue();
                    String statusCode = jobDescNode.get("StatusCode").textValue();
                    archiveSizeInBytes = jobDescNode.get("ArchiveSizeInBytes").longValue();
                    if (retrievedJobId.equals(jobId)) {
                        messageFound = true;
                        if (statusCode.equals("Succeeded")) {
                            jobSuccessful = true;
                        }
                    }
                }

            } else {
                Thread.sleep(sleepTime * 1000);
            }
        }
        return (messageFound && jobSuccessful) ? archiveSizeInBytes : -1;
    }

    private void cleanUp() {
        snsClient.unsubscribe(UnsubscribeRequest.builder().subscriptionArn(snsSubscriptionARN).build());
        snsClient.deleteTopic(DeleteTopicRequest.builder().topicArn(snsTopicARN).build());
        sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(sqsQueueURL).build());
    }

    @Override
    public void download(String accountId, String vaultName, String archiveId, String fileName) {

        String snsTopicName = "glacier-archive-retrieval" + System.currentTimeMillis();
        String sqsQueueName = "glacier-archive-retrieval" + System.currentTimeMillis();

        try {
            setupSQS(snsTopicName, sqsQueueName);

            setupSNS(snsTopicName);

            String jobId = initiateDownloadJobRequest(vaultName, archiveId);
            System.out.println("Jobid = " + jobId);

            Boolean success = waitForJobToComplete(jobId, sqsQueueURL);
            if (!success) { throw new Exception("Job did not complete successfully."); }

            downloadJobOutput(vaultName, jobId, fileName);

            cleanUp();

        } catch (Exception e) {
            System.err.println("Archive retrieval failed.");
            System.err.println(e);
        }
    }

    @Override
    public void downloadInChunks(String accountId, String vaultName, String archiveId, String fileName, long downloadChunkSize) {

        String snsTopicName = "glacier-archive-retrieval-in-chunks" + System.currentTimeMillis();
        String sqsQueueName = "glacier-archive-retrieval-in-chunks" + System.currentTimeMillis();
        String snsSubscriptionARN;

        try {
            setupSQS(snsTopicName, sqsQueueName);

            setupSNS(snsTopicName);

            String jobId = initiateDownloadJobRequest(vaultName, archiveId);
            System.out.println("Jobid = " + jobId);


            long archiveSizeInBytes = waitForJobToCompleteInChunks(jobId, sqsQueueURL);
            if (archiveSizeInBytes==-1) { throw new Exception("Job did not complete successfully."); }

            downloadJobOutput(vaultName, jobId, fileName, downloadChunkSize, archiveSizeInBytes);

            cleanUp();

        } catch (Exception e) {
            System.err.println("Archive retrieval failed.");
            System.err.println(e);
        }
    }

    private void downloadJobOutput(String vaultName, String jobId, String fileName) {
        GetJobOutputRequest request = GetJobOutputRequest.builder()
                .jobId(jobId)
                .vaultName(vaultName)
                .build();
        InputStream input = glacierClient.getJobOutputAsBytes(request).asInputStream();
        OutputStream output = null;
        try {
            File file = new File(fileName); // create file object
//            if (!file.exists()) {
//                System.out.println("specified file does not exist, creating file now...");
//                boolean createSuccess = file.createNewFile();
//                if (createSuccess) System.out.println("file successfully created!");
//            }
            System.out.println("Writing to file now...");
            output = new BufferedOutputStream(new FileOutputStream(file));

            byte[] buffer = new byte[1024 * 1024];

            int bytesRead = 0;
            do {
                bytesRead = input.read(buffer);
                if (bytesRead <= 0) break;
                output.write(buffer, 0, bytesRead);
            } while (bytesRead > 0);

            System.out.println("Retrieved archive to " + file.getAbsolutePath());
        } catch (IOException ioe) {
            System.out.println("ioe while outputing to file in downloading: " + ioe.getMessage());
        } finally {
            try {input.close();}
            catch (Exception e) {
                System.err.println("input closing in downloadJobOutput()");
                System.err.println(e);
            }
            try {output.close();}
            catch (Exception e) {
                System.err.println("output closing in downloadJobOutput()");
                System.err.println(e.getMessage());
            }
        }
    }

    private void downloadJobOutput(String vaultName, String jobId, String fileName, long downloadChunkSize, long archiveSizeInBytes) throws IOException {

        if (archiveSizeInBytes < 0) {
            System.err.println("Nothing to download.");
            return;
        }

        System.out.println("archiveSizeInBytes: " + archiveSizeInBytes);
        FileOutputStream fstream = new FileOutputStream(fileName);
        long startRange = 0;
        long endRange = (downloadChunkSize > archiveSizeInBytes) ? archiveSizeInBytes -1 : downloadChunkSize - 1;

        do {

            GetJobOutputRequest getJobOutputRequest = GetJobOutputRequest.builder()
                    .vaultName(vaultName)
                    .range("bytes=" + startRange + "-" + endRange)
                    .jobId(jobId).build();

            GetJobOutputResponse getJobOutputResponse = glacierClient.getJobOutput(getJobOutputRequest, Paths.get(fileName));

            BufferedInputStream is = new BufferedInputStream(glacierClient.getJobOutputAsBytes(getJobOutputRequest).asInputStream());
            byte[] buffer = new byte[(int)(endRange - startRange + 1)];

            System.out.println("Checksum received: " + getJobOutputResponse.checksum());
            System.out.println("Content range " + getJobOutputResponse.contentRange());


            int totalRead = 0;
            while (totalRead < buffer.length) {
                int bytesRemaining = buffer.length - totalRead;
                int read = is.read(buffer, totalRead, bytesRemaining);
                if (read > 0) {
                    totalRead = totalRead + read;
                } else {
                    break;
                }

            }
            System.out.println("Calculated checksum: " + TreeHashGenerator.calculateTreeHash(new ByteArrayInputStream(buffer)));
            System.out.println("read = " + totalRead);
            fstream.write(buffer);

            startRange = startRange + (long)totalRead;
            endRange = ((endRange + downloadChunkSize) >  archiveSizeInBytes) ? archiveSizeInBytes : (endRange + downloadChunkSize);
            is.close();
        } while (endRange <= archiveSizeInBytes  && startRange < archiveSizeInBytes);

        fstream.close();
        System.out.println("Retrieved file to " + fileName);

    }

    private class ArchiveTransferManagerBuilderImpl implements ArchiveTransferManager.Builder {
        @Override
        public ArchiveTransferManager.Builder glacierClient(GlacierClient glacierClient) {
            ArchiveTransferManagerImpl.glacierClient = glacierClient;
            return ArchiveTransferManager.builder().glacierClient(glacierClient);
            // => new ArchiveTransferManagerImpl().Builder().glacierClient(glacierClient)
            // => new ArchiveTransferManagerImpl().(new ArchiveTransferManagerBuilderImpl()).glacierClient(glacierClient)
            // =>
        }

        @Override
        public ArchiveTransferManager.Builder sqsClient(SqsClient sqsClient) {
            ArchiveTransferManagerImpl.sqsClient = sqsClient;
            return ArchiveTransferManager.builder().sqsClient(sqsClient);
        }

        @Override
        public ArchiveTransferManager.Builder snsClient(SnsClient snsClient) {
            ArchiveTransferManagerImpl.snsClient = snsClient;
            return ArchiveTransferManager.builder().snsClient(snsClient);
        }

        @Override
        public ArchiveTransferManager.Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            ArchiveTransferManagerImpl.credentialsProvider = credentialsProvider;
            return ArchiveTransferManager.builder().credentialsProvider(credentialsProvider);
        }

        @Override
        public ArchiveTransferManager.Builder clientOverrideConfiguration(ClientOverrideConfiguration clientOverrideConfiguration) {
            ArchiveTransferManagerImpl.clientOverrideConfiguration = clientOverrideConfiguration;
            return ArchiveTransferManager.builder().clientOverrideConfiguration(clientOverrideConfiguration);
        }

        @Override
        public ArchiveTransferManager build() {
            return ArchiveTransferManager.builder().build();
        }
    }

    @Override
    public void delete(String accountId, String vaultName, String archiveId) {
        try {
            DeleteArchiveRequest dRequest = DeleteArchiveRequest.builder()
                    .accountId(accountId)
                    .vaultName(vaultName)
                    .archiveId(archiveId)
                    .build();
            glacierClient.deleteArchive(dRequest);

            System.out.println("Deleted archive successfully.");
        } catch (Exception e) {
            System.err.println("Archive not deleted.");
            System.err.println(e);
        }
    }
}
