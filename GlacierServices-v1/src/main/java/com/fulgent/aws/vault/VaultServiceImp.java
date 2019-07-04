package com.fulgent.aws.vault;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.SQSActions;
import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.model.*;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.*;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VaultServiceImp implements VaultService{
    private static AmazonGlacier glacierClient = null;
    private static AmazonSQS sqsClient;
    private static AmazonSNS snsClient;
    private static String snsTopicName = "*** provide topic name ***";
    private static String sqsQueueName = "*** provide queue name ***";
    private static String sqsQueueARN;
    private static String sqsQueueURL;
    private static String snsTopicARN;
    private static String snsSubscriptionARN;
    private static String fileName = "*** provide file name ***";
    private static String region = "*** region ***";
    private static long sleepTime = 600;

    private static CreateVaultRequest cRequest = null;
    private static DeleteVaultRequest dRequest = null;

    private static String accountId = "";

    public VaultServiceImp(String accountId)
    {
        glacierClient = AmazonGlacierClientBuilder.defaultClient();
        this.accountId = accountId;
    }

    public AmazonGlacier getVaultServiceImp(String accountId)
    {
        if (glacierClient == null) {
            glacierClient = AmazonGlacierClientBuilder.defaultClient();
        }

        return glacierClient;
    }

    public List<DescribeVaultOutput> listVaults()
    {
        List<DescribeVaultOutput> result = new ArrayList<>();
        String marker = null;
        do {
            ListVaultsRequest request = new ListVaultsRequest()
                    .withLimit("5")
                    .withMarker(marker);
            ListVaultsResult listVaultsResult = this.glacierClient.listVaults(request);

            List<DescribeVaultOutput> vaultList = listVaultsResult.getVaultList();
            result.addAll(vaultList);
            marker = listVaultsResult.getMarker();
        } while (marker != null);

        return result;
    }

    public String createVault(String vaultName)
    {
        if (accountId != null) cRequest = new CreateVaultRequest().withAccountId(this.accountId).withVaultName(vaultName);
        else cRequest = new CreateVaultRequest().withVaultName(vaultName);

        CreateVaultResult result = glacierClient.createVault(cRequest);
//        System.out.println("vault successfully created!");
//        System.out.println("vault location: " + result.getLocation());
        return result.getLocation();
    }

    public void deleteVault(String vaultName)
    {
        if (accountId != null) dRequest = new DeleteVaultRequest().withAccountId(this.accountId).withVaultName(vaultName);
        else dRequest = new DeleteVaultRequest().withVaultName(vaultName);

        glacierClient.deleteVault(dRequest);

    }

    public void downloadVault(String vaultName) {
        sqsClient = AmazonSQSClientBuilder.defaultClient();
        snsClient = AmazonSNSClientBuilder.defaultClient();

        try {
            setupSQS();

            setupSNS();

            String jobId = initiateJobRequest(vaultName);
            System.out.println("Jobid = " + jobId);

            Boolean success = waitForJobToComplete(jobId, sqsQueueURL);
            if (!success) { throw new Exception("Job did not complete successfully."); }

            downloadJobOutput(jobId, vaultName);

            cleanUp();

        } catch (Exception e) {
            System.err.println("Inventory retrieval failed.");
            System.err.println(e);
        }
    }

    protected void downloadJobOutput(String jobId, String vaultName) throws IOException
    {
        GetJobOutputRequest getJobOutputRequest = new GetJobOutputRequest()
                .withVaultName(vaultName)
                .withJobId(jobId);
        GetJobOutputResult getJobOutputResult = glacierClient.getJobOutput(getJobOutputRequest);

        FileWriter fstream = new FileWriter(fileName);
        BufferedWriter out = new BufferedWriter(fstream);
        BufferedReader in = new BufferedReader(new InputStreamReader(getJobOutputResult.getBody()));
        String inputLine;
        try {
            while ((inputLine = in.readLine()) != null) {
                out.write(inputLine);
            }
        }catch(IOException e) {
            throw new AmazonClientException("Unable to save archive", e);
        }finally{
            try {in.close();}  catch (Exception e) {}
            try {out.close();}  catch (Exception e) {}
        }
        System.out.println("Retrieved inventory to " + fileName);
    }

    protected void setupSQS() {
        CreateQueueRequest request = new CreateQueueRequest()
                .withQueueName(sqsQueueName);
        CreateQueueResult result = sqsClient.createQueue(request);
        sqsQueueURL = result.getQueueUrl();

        GetQueueAttributesRequest qRequest = new GetQueueAttributesRequest()
                .withQueueUrl(sqsQueueURL)
                .withAttributeNames("QueueArn");

        GetQueueAttributesResult qResult = sqsClient.getQueueAttributes(qRequest);
        sqsQueueARN = qResult.getAttributes().get("QueueArn");

        Policy sqsPolicy =
                new Policy().withStatements(
                        new Statement(Statement.Effect.Allow)
                                .withPrincipals(Principal.AllUsers)
                                .withActions(SQSActions.SendMessage)
                                .withResources(new Resource(sqsQueueARN)));
        Map<String, String> queueAttributes = new HashMap<String, String>();
        queueAttributes.put("Policy", sqsPolicy.toJson());
        sqsClient.setQueueAttributes(new SetQueueAttributesRequest(sqsQueueURL, queueAttributes));

    }

    protected void setupSNS() {
        CreateTopicRequest request = new CreateTopicRequest()
                .withName(snsTopicName);
        CreateTopicResult result = snsClient.createTopic(request);
        snsTopicARN = result.getTopicArn();

        SubscribeRequest request2 = new SubscribeRequest()
                .withTopicArn(snsTopicARN)
                .withEndpoint(sqsQueueARN)
                .withProtocol("sqs");
        SubscribeResult result2 = snsClient.subscribe(request2);

        snsSubscriptionARN = result2.getSubscriptionArn();
    }

    protected String initiateJobRequest(String vaultName) {

        JobParameters jobParameters = new JobParameters()
                .withType("inventory-retrieval")
                .withSNSTopic(snsTopicARN);

        InitiateJobRequest request = new InitiateJobRequest()
                .withVaultName(vaultName)
                .withJobParameters(jobParameters);

        InitiateJobResult response = glacierClient.initiateJob(request);

        return response.getJobId();
    }

    protected Boolean waitForJobToComplete(String jobId, String sqsQueueUrl) throws InterruptedException, IOException {

        Boolean messageFound = false;
        Boolean jobSuccessful = false;
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();

        while (!messageFound) {
            List<Message> msgs = sqsClient.receiveMessage(
                    new ReceiveMessageRequest(sqsQueueUrl).withMaxNumberOfMessages(10)).getMessages();

            if (msgs.size() > 0) {
                for (Message m : msgs) {
                    JsonParser jpMessage = factory.createParser(m.getBody());
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

    protected void cleanUp() {
        snsClient.unsubscribe(new UnsubscribeRequest(snsSubscriptionARN));
        snsClient.deleteTopic(new DeleteTopicRequest(snsTopicARN));
        sqsClient.deleteQueue(new DeleteQueueRequest(sqsQueueURL));
    }
}
