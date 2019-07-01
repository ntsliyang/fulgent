package com.fulgent.aws.archive;

import com.amazonaws.services.glacier.model.DeleteArchiveResult;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.UploadResult;

public interface ArchiveService {
    public ArchiveTransferManager getArchiveServiceImp(String accountId, String vaultName);

    public UploadResult uploadArchive(String archiveFileNamePath);

    public DeleteArchiveResult deleteArchive(String archiveId);

    public void downloaddArchive(String archiveId);

}
