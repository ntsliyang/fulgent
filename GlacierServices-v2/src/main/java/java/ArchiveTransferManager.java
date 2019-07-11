import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.glacier.GlacierClient;
import software.amazon.awssdk.services.glacier.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.glacier.model.UploadArchiveResponse;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public interface ArchiveTransferManager {

    GlacierClient getGlacierClient();

    default String upload(String vaultName, String archiveDescription, String fileName) throws IOException {
        return this.upload(null, vaultName, archiveDescription, fileName);
    }

    String upload(String accountId, String vaultName, String archiveDescription, String fileName) throws IOException;

    default void download(String vaultName, String archiveId, String fileName) {
        this.download(null, vaultName, archiveId, fileName);
    }

    void download(String accountId, String vaultName, String archiveId, String fileName);

    void downloadInChunks(String accountId, String vaultName, String archiveId, String fileName, long downloadChunkSize);

    default void delete(String vaultName, String archiveId) {
        this.delete(null, vaultName, archiveId);
    }

    void delete(String accountId, String vaultName, String archiveId);

    static ArchiveTransferManager create() {return builder().build();}

    static ArchiveTransferManager.Builder builder() {return new ArchiveTransferManagerBuilder();}

    interface Builder {
        Builder glacierClient(GlacierClient glacierClient);

        Builder sqsClient(SqsClient sqsClient);

        Builder snsClient(SnsClient snsClient);

        Builder credentialsProvider(AwsCredentialsProvider credentialsProvider);

        Builder clientOverrideConfiguration(ClientOverrideConfiguration clientOverrideConfiguration);

        ArchiveTransferManager build();
    }

}