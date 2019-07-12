package archive;

import software.amazon.awssdk.services.glacier.GlacierClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

public class ArchiveTransferManagerParams {
    private GlacierClient amazonGlacier;

    private SqsClient amazonSQS;

    private SnsClient amazonSNS;

    public GlacierClient getAmazonGlacier() {
        return amazonGlacier;
    }

    public void setAmazonGlacier(GlacierClient amazonGlacier) {
        this.amazonGlacier = amazonGlacier;
    }

    public ArchiveTransferManagerParams withAmazonGlacier(GlacierClient amazonGlacier) {
        setAmazonGlacier(amazonGlacier);
        return this;
    }

    public SqsClient getAmazonSQS() {
        return amazonSQS;
    }

    public void setAmazonSQS(SqsClient amazonSQS) {
        this.amazonSQS = amazonSQS;
    }

    public ArchiveTransferManagerParams withAmazonSQS(SqsClient amazonSQS) {
        setAmazonSQS(amazonSQS);
        return this;
    }

    public SnsClient getAmazonSNS() {
        return amazonSNS;
    }

    public void setAmazonSNS(SnsClient amazonSNS) {
        this.amazonSNS = amazonSNS;
    }

    public ArchiveTransferManagerParams withAmazonSNS(SnsClient amazonSNS) {
        setAmazonSNS(amazonSNS);
        return this;
    }
}
