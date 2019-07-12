package vault;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.glacier.GlacierClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

public interface VaultTransferManager {

    String create(String accountId, String vaultName); // returns the location of vault created

    void describe(String accountId, String vaultName); // describe the vault specified

    void list();

    void getNotifications(String accountId, String vaultName);

    void setNotifications(String vaultName);

    void deleteNotifications(String accountId, String vaultName);

    void downloadInventory(String vaultName, String fileName);

    void delete(String vaultName);

    static VaultTransferManager create() {return builder().build();}

    static VaultTransferManager.Builder builder() {return new VaultTransferManagerBuilder();}

    interface Builder {
        VaultTransferManager.Builder glacierClient(GlacierClient glacierClient);

        VaultTransferManager.Builder sqsClient(SqsClient sqsClient);

        VaultTransferManager.Builder snsClient(SnsClient snsClient);

        VaultTransferManager.Builder credentialsProvider(AwsCredentialsProvider credentialsProvider);

        VaultTransferManager.Builder clientOverrideConfiguration(ClientOverrideConfiguration clientOverrideConfiguration);

        VaultTransferManager build();
    }
}
