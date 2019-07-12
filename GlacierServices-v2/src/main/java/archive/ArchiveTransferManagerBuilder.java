package archive;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.glacier.GlacierClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

public class ArchiveTransferManagerBuilder implements ArchiveTransferManager.Builder {

    private GlacierClient glacierClient;
    private SqsClient sqsClient;
    private SnsClient snsClient;
    private AwsCredentialsProvider credentialsProvider;
    private ClientOverrideConfiguration clientOverrideConfiguration;

    @Override
    public ArchiveTransferManager.Builder glacierClient(GlacierClient glacierClient) {
        this.glacierClient = glacierClient;
        return this;
    }

    @Override
    public ArchiveTransferManager.Builder sqsClient(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
        return this;
    }

    @Override
    public ArchiveTransferManager.Builder snsClient(SnsClient snsClient) {
        this.snsClient = snsClient;
        return this;
    }

    @Override
    public ArchiveTransferManager.Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        return this;
    }

    @Override
    public ArchiveTransferManager.Builder clientOverrideConfiguration(ClientOverrideConfiguration clientOverrideConfiguration) {
        this.clientOverrideConfiguration = clientOverrideConfiguration;
        return this;
    }

    @Override
    public ArchiveTransferManager build() {
        return new ArchiveTransferManagerImpl(
                glacierClient,
                sqsClient,
                snsClient,
                credentialsProvider,
                clientOverrideConfiguration
        );
    }
}
