package vault;

import archive.ArchiveTransferManager;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.glacier.GlacierClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

public class VaultTransferManagerBuilder implements VaultTransferManager.Builder {

    private GlacierClient glacierClient;
    private SqsClient sqsClient;
    private SnsClient snsClient;
    private AwsCredentialsProvider credentialsProvider;
    private ClientOverrideConfiguration clientOverrideConfiguration;

    @Override
    public VaultTransferManager.Builder glacierClient(GlacierClient glacierClient) {
        this.glacierClient = glacierClient;
        return this;
    }

    @Override
    public VaultTransferManager.Builder sqsClient(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
        return this;
    }

    @Override
    public VaultTransferManager.Builder snsClient(SnsClient snsClient) {
        this.snsClient = snsClient;
        return this;
    }

    @Override
    public VaultTransferManager.Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        return this;
    }

    @Override
    public VaultTransferManager.Builder clientOverrideConfiguration(ClientOverrideConfiguration clientOverrideConfiguration) {
        this.clientOverrideConfiguration = clientOverrideConfiguration;
        return this;
    }

    @Override
    public VaultTransferManager build() {
        return new VaultTransferManagerImpl(
                glacierClient,
                sqsClient,
                snsClient,
                credentialsProvider,
                clientOverrideConfiguration
        );
    }
}
