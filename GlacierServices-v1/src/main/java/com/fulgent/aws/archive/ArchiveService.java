package com.fulgent.aws.archive;

import com.amazonaws.services.glacier.model.DeleteArchiveResult;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.UploadResult;

public interface ArchiveService {
    public ArchiveTransferManager getArchiveServiceImp(String accountId);

    public UploadResult uploadArchive(String archiveFileNamePath, String vaultName, String archiveDescription, String filePath) throws Exception;

    public DeleteArchiveResult deleteArchive(String archiveId, String vaultName);

    public void downloaddArchive(String archiveId, String vaultName, String filePath);

}
