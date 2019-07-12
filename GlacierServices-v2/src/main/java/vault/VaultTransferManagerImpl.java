package vault;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.glacier.GlacierClient;
import software.amazon.awssdk.services.glacier.model.*;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import software.amazon.awssdk.services.sqs.model.AddPermissionRequest;
import software.amazon.awssdk.services.sqs.model.AddPermissionResponse;

import java.io.*;
import java.util.List;

public class VaultTransferManagerImpl implements VaultTransferManager {

    private GlacierClient glacierClient;

    private SqsClient sqsClient;

    private SnsClient snsClient;

    private AwsCredentialsProvider credentialsProvider;

    private ClientOverrideConfiguration clientOverrideConfiguration;

    private static long sleepTime = 600;

    private static String sqsQueueARN;
    private static String sqsQueueURL;
    private static String snsTopicARN;
    private static String snsSubscriptionARN;

    public VaultTransferManagerImpl(
            GlacierClient glacierClient,
            SqsClient sqsClient,
            SnsClient snsClient,
            AwsCredentialsProvider credentialsProvider,
            ClientOverrideConfiguration clientOverrideConfiguration
    ) {
        this.glacierClient = glacierClient;
        this.sqsClient = sqsClient;
        this.snsClient = snsClient;
        this.credentialsProvider = credentialsProvider;
        this.clientOverrideConfiguration = clientOverrideConfiguration;
    }

    @Override
    public String create(String accountId, String vaultName) {
        CreateVaultRequest createVaultRequest = null;
        if (accountId != null) createVaultRequest = CreateVaultRequest.builder()
                .accountId(accountId)
                .vaultName(vaultName)
                .build();
        else createVaultRequest = CreateVaultRequest.builder()
                .vaultName(vaultName)
                .build();
        CreateVaultResponse createVaultResult = glacierClient.createVault(createVaultRequest);

        System.out.println("Created vault successfully: " + createVaultResult.location());

        return createVaultResult.location();
    }

    @Override
    public void describe(String accountId, String vaultName) {
        DescribeVaultRequest describeVaultRequest = DescribeVaultRequest.builder()
                .accountId(accountId)
                .vaultName(vaultName)
                .build();
        DescribeVaultResponse describeVaultResult  = glacierClient.describeVault(describeVaultRequest);

        System.out.println("Describing the vault: " + vaultName);
        System.out.print(
                "CreationDate: " + describeVaultResult.creationDate() +
                        "\nLastInventoryDate: " + describeVaultResult.lastInventoryDate() +
                        "\nNumberOfArchives: " + describeVaultResult.numberOfArchives() +
                        "\nSizeInBytes: " + describeVaultResult.sizeInBytes() +
                        "\nVaultARN: " + describeVaultResult.vaultARN() +
                        "\nVaultName: " + describeVaultResult.vaultARN());
    }

    /**
     * Lists all the vaults of the user's current account in Amazon Glacier
     */
    @Override
    public void list() {
        boolean list_complete = false;
        String new_marker = null;
        int total_vaults = 0;
        System.out.println("Your Amazon Glacier vaults:");
        while (!list_complete) {
            ListVaultsResponse response = null;

            if (new_marker != null) {
                ListVaultsRequest request = ListVaultsRequest.builder()
                        .marker(new_marker)
                        .build();
                response = glacierClient.listVaults(request);
            } else {
                ListVaultsRequest request = ListVaultsRequest.builder()
                        .build();
                response = glacierClient.listVaults(request);
            }

            List<DescribeVaultOutput> vault_list = response.vaultList();
            for (DescribeVaultOutput v : vault_list) {
                total_vaults += 1;
                System.out.println("* " + v.vaultName());
            }
            // check for further results.
            new_marker = response.marker();
            if (new_marker == null) {
                list_complete = true;
            }
        }

        if (total_vaults == 0) {
            System.out.println("  no vaults found.");
        }
    }

    @Override
    public void setNotifications(String vaultName) {
        VaultNotificationConfig config = VaultNotificationConfig.builder()
                .snsTopic(snsTopicARN)
                .events("ArchiveRetrievalCompleted", "InventoryRetrievalCompleted").build();

        SetVaultNotificationsRequest request = SetVaultNotificationsRequest.builder()
                .vaultName(vaultName)
                .vaultNotificationConfig(config).build();

        glacierClient.setVaultNotifications(request);
        System.out.println("Notification configured for vault: " + vaultName);
    }

    @Override
    public void getNotifications(String accountId, String vaultName) {
        VaultNotificationConfig notificationConfig = null;
        GetVaultNotificationsRequest request = GetVaultNotificationsRequest.builder()
                .accountId(accountId)
                .vaultName(vaultName).build();
        GetVaultNotificationsResponse result = glacierClient.getVaultNotifications(request);
        notificationConfig = result.vaultNotificationConfig();

        System.out.println("Notifications configuration for vault: "
                + vaultName);
        System.out.println("Topic: " + notificationConfig.snsTopic());
        System.out.println("Events: " + notificationConfig.events());
    }

    @Override
    public void deleteNotifications(String accountId, String vaultName) {
        DeleteVaultNotificationsRequest request = DeleteVaultNotificationsRequest.builder()
                .accountId(accountId)
                .vaultName(vaultName).build();
        glacierClient.deleteVaultNotifications(request);
        System.out.println("Notifications configuration deleted for vault: " + vaultName);
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

    private String initiateJobRequest(String vaultName) {
        InitiateJobRequest request = InitiateJobRequest.builder()
                .vaultName(vaultName)
                .jobParameters(JobParameters.builder()
                        .snsTopic(snsTopicARN)
                        .type("vault-inventory-retrieval")
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

    private void downloadJobOutput(String vaultName, String jobId, String fileName) throws IOException{
        GetJobOutputRequest getJobOutputRequest = GetJobOutputRequest.builder()
                .vaultName(vaultName)
                .jobId(jobId).build();
        InputStream instream = glacierClient.getJobOutputAsBytes(getJobOutputRequest).asInputStream();

        FileWriter fstream = new FileWriter(fileName);
        BufferedWriter out = new BufferedWriter(fstream);
        BufferedReader in = new BufferedReader(new InputStreamReader(instream));
        String inputLine;
        try {
            while ((inputLine = in.readLine()) != null) {
                out.write(inputLine);
            }
        }catch(IOException e) {
           System.out.println(e);
        }finally{
            try {in.close();}  catch (Exception e) {}
            try {out.close();}  catch (Exception e) {}
        }
        System.out.println("Retrieved inventory to " + fileName);
    }

    private void cleanUp() {
        snsClient.unsubscribe(UnsubscribeRequest.builder().subscriptionArn(snsSubscriptionARN).build());
        snsClient.deleteTopic(DeleteTopicRequest.builder().topicArn(snsTopicARN).build());
        sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(sqsQueueURL).build());
    }

    @Override
    public void downloadInventory(String vaultName, String fileName) {
        String snsTopicName = "glacier-vault-inventory-retrieval-" + System.currentTimeMillis();
        String sqsQueueName = "glacier-vault-inventory-retrieval-" + System.currentTimeMillis();

        try {
            setupSQS(snsTopicName, sqsQueueName);

            setupSNS(snsTopicName);

            String jobId = initiateJobRequest(vaultName);
            System.out.println("Jobid = " + jobId);

            Boolean success = waitForJobToComplete(jobId, sqsQueueURL);
            if (!success) { throw new Exception("Job did not complete successfully."); }

            downloadJobOutput(vaultName, jobId, fileName);


        } catch (Exception e) {
            System.err.println("Inventory retrieval failed.");
            System.err.println(e);
        }
    }

    @Override
    public void delete(String vaultName) {
        try {
            DeleteVaultRequest request = DeleteVaultRequest.builder()
                    .vaultName(vaultName)
                    .build();

            glacierClient.deleteVault(request);
            System.out.println("Deleted vault: " + vaultName);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
