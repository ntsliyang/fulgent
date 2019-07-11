import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.glacier.GlacierClient;
import software.amazon.awssdk.services.glacier.model.*;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

//    private static final Logger logger = LogManager.getLogger(Main.class);
//    private static final GlacierClient glacierClient = GlacierClient.create();
//    private static final SqsClient sqsClient = SqsClient.create();
//    private static final SnsClient snsClient = SnsClient.create();
//    private static final IamClient iamClient = IamClient.builder().region(Region.AWS_GLOBAL).build();
//
//    private static String archiveId = "q45i9Xiuw0FxbTuU8_1CQWgNjdjjR6JwAY7tYMLd4xQJoyEwLpxoGSyqeCoJnhciL8zK4xMO0yYmEhBZNrHcU0KKB5u1fzry8nyYsDW1eI__8hOW93NroxsxCipAibz8AQyiEN7IGw";
//    private static String vaultName = "example-vault";
//    private static String snsTopicName;
//    private static String sqsQueueName;
//    private static String sqsQueueARN;
//    private static String sqsQueueURL;
//    private static String snsTopicARN;
//    private static String snsSubscriptionARN;
//    private static String fileName = "/Users/apple/Downloads/download.jpg";
//    private static final String POLICY_DOCUMENT =
//            "{" +
//                    "  \"Version\": \"2019-06-26\","+
//                    "  \"Statement\": [" +
//                    "    {" +
//                    "        \"Principal\": \"AllUsers\"," +
//                    "        \"Effect\": \"Allow\"," +
//                    "        \"Action\": \"sqs:SendMessage\"," +
//                    "        \"Resource\": \"*\"" +
//                    "    }," +
//                    "   ]" +
//                    "}";
//
//    public static long sleepTime = 600;

//    private static void ListVaults() {
//        int total_vaults = 0;
//
//        ListVaultsResponse response = glacierClient.listVaults();
//        List<DescribeVaultOutput> vault_list = response.vaultList();
//        for (DescribeVaultOutput v : vault_list) {
//            total_vaults += 1;
//            System.out.println("* " + v.vaultName());
//        }
//
//        if (total_vaults == 0) {
//            System.out.println(" no vaults found.");
//        }
//    }
//
//    private static void UploadSingleOperation(String archiveToUpload) {
//        UploadArchiveRequest request = UploadArchiveRequest.builder().vaultName(vaultName).build();
//        UploadArchiveResponse response = glacierClient.uploadArchive(request, Paths.get(archiveToUpload));
//        System.out.println("Archive ID: " + response.archiveId());
//    }
//
//    private static void UploadMultiPart(String archiveToUpload) {
//        UploadMultipartPartRequest request = UploadMultipartPartRequest.builder().vaultName(vaultName).build();
//        UploadMultipartPartResponse response = glacierClient.uploadMultipartPart(request, Paths.get(archiveToUpload));
//        System.out.println("Archive checksum: " + response.checksum());
//    }
//
//    private static void setupSQS() {
//        CreateQueueRequest request = CreateQueueRequest.builder().queueName(sqsQueueName).build();
//        CreateQueueResponse response = sqsClient.createQueue(request);
//        sqsQueueURL = response.queueUrl();
//
//        GetQueueAttributesRequest qRequest = GetQueueAttributesRequest.builder()
//                .queueUrl(sqsQueueURL)
//                .attributeNames(QueueAttributeName.fromValue("QueueArn"))
//                .build();
//
//        GetQueueAttributesResponse qResponse = sqsClient.getQueueAttributes(qRequest);
//        sqsQueueARN = qResponse.attributesAsStrings().get("QueueArn");
//
//        AddPermissionRequest aRequest = AddPermissionRequest.builder()
//                .awsAccountIds(iamClient.getUser().user().userId())
//                .label("sqsPolicy")
//                .actions("SendMessage")
//                .queueUrl(sqsQueueURL)
//                .build();
//        AddPermissionResponse aResponse = sqsClient.addPermission(aRequest);
//        System.out.println("--------");
////        aResponse.
////        System.out.println("sqsQueueARN: " + sqsQueueARN);
////        CreatePolicyRequest pRequest = CreatePolicyRequest.builder()
////                .policyName("sqsPolicy")
////                .policyDocument(POLICY_DOCUMENT)
////                .build();
////        CreatePolicyResponse pResponse = iamClient.createPolicy(pRequest);
////        Policy sqsPolicy = pResponse.policy();
////        Map<String, String> queueAttributes = new HashMap<>();
////        System.out.println(sqsPolicy.toString());
////        queueAttributes.put("Policy", POLICY_DOCUMENT);
////        sqsClient.setQueueAttributes(SetQueueAttributesRequest.builder().attributesWithStrings(queueAttributes).queueUrl(sqsQueueURL).build());
//    }
//
//    private static void setupSNS() {
//        CreateTopicRequest request = CreateTopicRequest.builder()
//                .name(snsTopicName)
//                .build();
//        CreateTopicResponse response = snsClient.createTopic(request);
//        snsTopicARN = response.topicArn();
//
//        SubscribeRequest request2 = SubscribeRequest.builder()
//                .topicArn(snsTopicARN)
//                .endpoint(sqsQueueARN)
//                .protocol("sqs")
//                .build();
//        SubscribeResponse response2 = snsClient.subscribe(request2);
//
//        snsSubscriptionARN = response2.subscriptionArn();
//    }
//
//    private static String InitiateDownloadJobRequest() {
//        InitiateJobRequest request = InitiateJobRequest.builder()
//                .vaultName(vaultName)
//                .jobParameters(JobParameters.builder()
//                        .archiveId(archiveId)
//                        .snsTopic(snsTopicARN)
//                        .type("archive-retrieval")
//                        .build())
//                .build();
//        InitiateJobResponse response = glacierClient.initiateJob(request);
//        return response.jobId();
//    }
//
//    private String getQueueUrl(String accountId, String queueName) {
//        GetQueueUrlRequest request = GetQueueUrlRequest.builder()
//                .queueOwnerAWSAccountId(accountId)
//                .queueName(queueName)
//                .build();
//        GetQueueUrlResponse response = sqsClient.getQueueUrl(request);
//        return response.queueUrl();
//    }



    public static void main(String[] args) {
        // ListTopicsRequest request = ListTopicsRequest.builder().build();
//        ListTopicsResponse tResponse = snsClient.listTopics();
//        ListQueuesResponse qResponse = sqsClient.listQueues();
//        String[] splitSnsTopicName = tResponse.topics().get(0).topicArn().split(":");
//        snsTopicName = splitSnsTopicName[splitSnsTopicName.length - 1];
//        String[] splitSqsQueueName = qResponse.queueUrls().get(0).split("/");
//        sqsQueueName = splitSqsQueueName[splitSqsQueueName.length - 1];
//        if (snsTopicName.equals(sqsQueueName)) {
//            DownloadFiles();
//        }
        GlacierClient glacierClient = GlacierClient.builder().build();
        SqsClient sqsClient = SqsClient.create();
        SnsClient snsClient = SnsClient.create();
        ArchiveTransferManager atm = ArchiveTransferManager.builder()
                .glacierClient(glacierClient)
                .sqsClient(sqsClient)
                .snsClient(snsClient)
                .build();
        System.out.println(atm.getGlacierClient() == null);
//        System.out.println(atm.getGlacierClient() == null);
//        System.out.println(atm.getGlacierClient() == null);
    }
}
