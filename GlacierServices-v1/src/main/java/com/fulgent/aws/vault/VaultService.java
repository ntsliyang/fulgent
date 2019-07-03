package com.fulgent.aws.vault;

import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.model.DescribeVaultOutput;

import java.io.IOException;
import java.util.List;

public interface VaultService {
    AmazonGlacier getVaultServiceImp(String accountId);

    List<DescribeVaultOutput> listVaults(); // return names of vaults

    String createVault(String vaultName); // return location of newly created vault

    void deleteVault(String vaultName);

    void downloadVault(String vaultName, String fileName);

    void downloadJobOutput(String jobId, String vaultName) throws IOException;

    void setupSNS();

    void setupSQS();

    String initiateJobRequest(String vaultName);

    Boolean waitForJobToComplete(String jobId, String sqsQueueUrl) throws InterruptedException, IOException;

    void cleanUp();
}
