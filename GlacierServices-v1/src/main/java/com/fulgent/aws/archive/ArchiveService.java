package com.fulgent.aws.archive;

import com.amazonaws.services.glacier.model.DeleteArchiveResult;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManagerBuilder;
import com.amazonaws.services.glacier.transfer.UploadResult;

public interface ArchiveService {
    ArchiveTransferManager getArchiveServiceImp(String accountId);

    static ArchiveTransferManager create() {return builder().build();}

    static ArchiveTransferManagerBuilder builder() {return new ArchiveTransferManagerBuilder();}

    String uploadArchive(String vaultName, String archiveDescription, String filePath) throws Exception;

    void deleteArchive(String archiveId, String vaultName);

    void downloadArchive(String archiveId, String vaultName, String filePath);

}
