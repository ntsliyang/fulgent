//import software.amazon.awssdk.annotations.SdkInternalApi;
//import software.amazon.awssdk.auth.credentials.AwsCredentials;
//import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
//import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
//import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
//import software.amazon.awssdk.core.exception.SdkClientException;
//import software.amazon.awssdk.services.glacier.GlacierClient;
//import software.amazon.awssdk.services.glacier.model.*;
//
//import java.io.*;
//import java.util.List;
//
//public class VaultTransferManager {
//    private final GlacierClient glacierClient;
//
//    private final AwsCredentialsProvider credentialsProvider;
//
//    private final ClientOverrideConfiguration clientOverrideConfiguration;
//
//    public VaultTransferManager(AwsCredentials credentials) {
//        this(StaticCredentialsProvider.create(credentials), ClientOverrideConfiguration.builder().build());
//    }
//
//    public VaultTransferManager(AwsCredentialsProvider credentialsProvider, ClientOverrideConfiguration clientOverrideConfiguration) {
//        this(GlacierClient.builder().credentialsProvider(credentialsProvider).overrideConfiguration(clientOverrideConfiguration).build(),
//                credentialsProvider, clientOverrideConfiguration);
//    }
//
//    public VaultTransferManager(GlacierClient glacier, AwsCredentialsProvider credentialsProvider) {
//        this(glacier, credentialsProvider, ClientOverrideConfiguration.builder().build());
//    }
//
//    public VaultTransferManager(GlacierClient glacier, AwsCredentialsProvider credentialsProvider, ClientOverrideConfiguration clientOverrideConfiguration) {
//        this.glacierClient = glacier;
//        this.credentialsProvider = credentialsProvider;
//        this.clientOverrideConfiguration = clientOverrideConfiguration;
//    }
//
//    @SdkInternalApi
//    VaultTransferManager(VaultTransferManagerParams params) {
//        this.credentialsProvider = null;
//        this.clientOverrideConfiguration = null;
////        this.glacierClient = params.getAmazonGlacier();
////        this.sqsClient = params.getAmazonSQS();
////        this.snsClient = params.getAmazonSNS();
//    }
//
//    /**
//     * Lists all the vaults of the user's current account in Amazon Glacier
//     */
//    public void listVaults() {
//        boolean list_complete = false;
//        String new_marker = null;
//        int total_vaults = 0;
//        System.out.println("Your Amazon Glacier vaults:");
//        while (!list_complete) {
//            ListVaultsResponse response = null;
//
//            if (new_marker != null) {
//                ListVaultsRequest request = ListVaultsRequest.builder()
//                        .marker(new_marker)
//                        .build();
//                response = glacierClient.listVaults(request);
//            }
//            else {
//                ListVaultsRequest request = ListVaultsRequest.builder()
//                        .build();
//                response = glacierClient.listVaults(request);
//            }
//
//            List<DescribeVaultOutput> vault_list = response.vaultList();
//            for (DescribeVaultOutput v: vault_list) {
//                total_vaults += 1;
//                System.out.println("* " + v.vaultName());
//            }
//            // check for further results.
//            new_marker = response.marker();
//            if (new_marker == null) {
//                list_complete = true;
//            }
//        }
//
//        if (total_vaults == 0) {
//            System.out.println("  no vaults found.");
//        }
//    }
//
//    public void create(String accountId, String vaultName) {
//        CreateVaultRequest createVaultRequest = CreateVaultRequest.builder()
//                .accountId(accountId)
//                .vaultName(vaultName)
//                .build();
//        CreateVaultResponse createVaultResult = glacierClient.createVault(createVaultRequest);
//
//        System.out.println("Created vault successfully: " + createVaultResult.location());
//    }
//
//    public void describe(String accountId, String vaultName) {
//        DescribeVaultRequest describeVaultRequest = DescribeVaultRequest.builder()
//                .accountId(accountId)
//                .vaultName(vaultName)
//                .build();
//        DescribeVaultResponse describeVaultResult  = glacierClient.describeVault(describeVaultRequest);
//
//        System.out.println("Describing the vault: " + vaultName);
//        System.out.print(
//                "CreationDate: " + describeVaultResult.creationDate() +
//                        "\nLastInventoryDate: " + describeVaultResult.lastInventoryDate() +
//                        "\nNumberOfArchives: " + describeVaultResult.numberOfArchives() +
//                        "\nSizeInBytes: " + describeVaultResult.sizeInBytes() +
//                        "\nVaultARN: " + describeVaultResult.vaultARN() +
//                        "\nVaultName: " + describeVaultResult.vaultARN());
//    }
//
//    public void retrieve() {
//
//    }
//
//    private void setVaultNotifications(String vaultName) {
//        VaultNotificationConfig config = VaultNotificationConfig.builder()
//                .snsTopic(snsTopicARN)
//                .events("ArchiveRetrievalCompleted", "InventoryRetrievalCompleted").build();
//
//        SetVaultNotificationsRequest request = SetVaultNotificationsRequest.builder()
//                .vaultName(vaultName)
//                .vaultNotificationConfig(config).build();
//
//        glacierClient.setVaultNotifications(request);
//        System.out.println("Notification configured for vault: " + vaultName);
//    }
//
//    private void getVaultNotifications(String accountId, String vaultName) {
//        VaultNotificationConfig notificationConfig = null;
//        GetVaultNotificationsRequest request = GetVaultNotificationsRequest.builder()
//                .accountId(accountId)
//                .vaultName(vaultName).build();
//        GetVaultNotificationsResponse result = glacierClient.getVaultNotifications(request);
//        notificationConfig = result.vaultNotificationConfig();
//
//        System.out.println("Notifications configuration for vault: "
//                + vaultName);
//        System.out.println("Topic: " + notificationConfig.snsTopic());
//        System.out.println("Events: " + notificationConfig.events());
//    }
//
//    private void deleteVaultNotifications(String accountId, String vaultName) {
//        DeleteVaultNotificationsRequest request = DeleteVaultNotificationsRequest.builder()
//                .accountId(accountId)
//                .vaultName(vaultName).build();
//        glacierClient.deleteVaultNotifications(request);
//        System.out.println("Notifications configuration deleted for vault: " + vaultName);
//    }
//
//    public void download(String accountId, String vaultName) {
//        JobStatusMonitor jobStatusMonitor = null;
//        String jobId = null;
//
//        try {
//            if (credentialsProvider != null && clientOverrideConfiguration != null) {
//                jobStatusMonitor = new JobStatusMonitor(credentialsProvider, clientOverrideConfiguration);
//            }
//            else if (credentialsProvider != null) {
//                jobStatusMonitor = new JobStatusMonitor(credentialsProvider);
//            }
//            else {
//                jobStatusMonitor = new JobStatusMonitor();
//            }
//
//            JobParameters jobParameters = JobParameters.builder()
//                    .type("inventory-retrieval")
//                    .snsTopic(jobStatusMonitor.getTopicArn()).build();
//            InitiateJobResponse archiveRetrievalResult =
//                    glacierClient.initiateJob(InitiateJobRequest.builder()
//                            .accountId(accountId)
//                            .vaultName(vaultName)
//                            .jobParameters(jobParameters).build());
//            jobId = archiveRetrievalResult.jobId();
//            System.out.println("Jobid = " + jobId);
//
//            System.out.println("waiting for job to complete...");
//            boolean success = jobStatusMonitor.waitForJobToComplete(jobId);
//            if (!success) {throw new Exception("Job did not complete successfully.");}
//
//            System.out.println("downloading job output...");
//            downloadJobOutput(accountId, vaultName, jobId, file);
//
//            System.out.println("cleaning up sns and sqs identifiers...");
//            jobStatusMonitor.cleanup();
//        }
//    }
//
//    private void downloadJobOutput(String accountId, String vaultName, String jobId, File file) {
//        GetJobOutputRequest getJobOutputRequest = GetJobOutputRequest.builder()
//                .vaultName(vaultName)
//                .jobId(jobId).build();
//        InputStream input = glacierClient.getJobOutputAsBytes(getJobOutputRequest).asInputStream();
//
//        FileWriter fstream = new FileWriter(fileName);
//        BufferedWriter out = new BufferedWriter(fstream);
//        BufferedReader in = new BufferedReader(new InputStreamReader(input));
//        String inputLine;
//        try {
//            while ((inputLine = in.readLine()) != null) {
//                out.write(inputLine);
//            }
//        }catch(IOException e) {
//            throw SdkClientException.create("Unable to save archive", e);
//        }finally{
//            try {in.close();}
//            catch (Exception e) {
//                System.err.println("input closing in downloadJobOutput()");
//                System.err.println(e);
//            }
//            try {out.close();}
//            catch (Exception e) {
//                System.err.println("output closing in downloadJobOutput()");
//                System.err.println(e);
//            }
//        }
//        System.out.println("Retrieved inventory to " + file.getAbsolutePath());
//    }
//
//    public void delete(String accountId, String vaultName) {
//        DeleteVaultRequest request = DeleteVaultRequest.builder()
//                .accountId(accountId)
//                .vaultName(vaultName)
//                .build();
//        DeleteVaultResponse response = glacierClient.deleteVault(request);
//        System.out.println("Deleted vault: " + vaultName);
//    }
//}
