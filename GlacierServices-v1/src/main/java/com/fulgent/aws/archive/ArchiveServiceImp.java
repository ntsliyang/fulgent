package com.fulgent.aws.archive;

import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.model.DeleteArchiveRequest;
import com.amazonaws.services.glacier.model.DeleteArchiveResult;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManagerBuilder;
import com.amazonaws.services.glacier.transfer.UploadResult;

import java.io.File;

public class ArchiveServiceImp {
    ArchiveTransferManager atm = null;
    String accountId = "";

    private ArchiveServiceImp(String accountId) {
        ArchiveTransferManager atm = new ArchiveTransferManagerBuilder().build();
    }

    public ArchiveTransferManager getArchiveServiceImp(String accountId) {
        if (atm == null) {
            atm =new ArchiveTransferManagerBuilder().build();
            this.accountId = accountId;
        }

        return atm;
    }

    public UploadResult uploadArchive(String archiveFileNamePath, String vaultName, String archiveDescription, String filePath)
            throws Exception
    {
        System.out.println("atm created!");
        UploadResult result = atm.upload(vaultName, archiveDescription, new File(filePath));
        System.out.println("archived successfully created!");
        System.out.println("archive ID: " + result.getArchiveId());

        return result;
    }

    public DeleteArchiveResult deleteArchive(String archiveId, String vaultName)
    {
        DeleteArchiveRequest request = null;
        AmazonGlacier glacierClient = AmazonGlacierClientBuilder.defaultClient();
        if (accountId != null) {
            request = new DeleteArchiveRequest().withAccountId(this.accountId).withArchiveId(archiveId).withVaultName(vaultName);
        } else {
            request = new DeleteArchiveRequest().withArchiveId(archiveId).withVaultName(vaultName);
        }

        DeleteArchiveResult result = glacierClient.deleteArchive(request);
        System.out.println("archive successfully deleted!");

        return result;
    }

    public void downloaddArchive(String archiveId, String vaultName, String filePath)
    {
        ArchiveTransferManager atm = new ArchiveTransferManagerBuilder().build();
        System.out.println("atm created!");
        if (accountId != null) atm.download(accountId, vaultName, archiveId, new File(filePath));
        else atm.download(vaultName, archiveId, new File(filePath));
        System.out.println("file successfully downloaded!");
    }

}
