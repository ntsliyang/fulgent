package com.fulgent.aws.archive;

import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.model.DeleteArchiveRequest;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManagerBuilder;
import com.amazonaws.services.glacier.transfer.UploadResult;

import java.io.File;

public class ArchiveServiceImp implements ArchiveService{
    ArchiveTransferManager atm = null;
    String accountId = "";

    public ArchiveServiceImp(String accountId) {
        ArchiveTransferManager atm = new ArchiveTransferManagerBuilder().build();
    }

    public ArchiveTransferManager getArchiveServiceImp(String accountId) {
        if (atm == null) {
            atm =new ArchiveTransferManagerBuilder().build();
            this.accountId = accountId;
        }

        return atm;
    }

    public String uploadArchive(String vaultName, String archiveDescription, String filePath)
            throws Exception
    {
        UploadResult result = atm.upload(vaultName, archiveDescription, new File(filePath));

        return result.getArchiveId();
    }

    public void deleteArchive(String archiveId, String vaultName)
    {
        DeleteArchiveRequest request = null;
        AmazonGlacier glacierClient = AmazonGlacierClientBuilder.defaultClient();
        if (accountId != null) {
            request = new DeleteArchiveRequest().withAccountId(this.accountId).withArchiveId(archiveId).withVaultName(vaultName);
        } else {
            request = new DeleteArchiveRequest().withArchiveId(archiveId).withVaultName(vaultName);
        }

        glacierClient.deleteArchive(request);
    }

    public void downloadArchive(String archiveId, String vaultName, String filePath)
    {
        ArchiveTransferManager atm = new ArchiveTransferManagerBuilder().build();
        if (accountId != null) atm.download(accountId, vaultName, archiveId, new File(filePath));
        else atm.download(vaultName, archiveId, new File(filePath));
    }

}
