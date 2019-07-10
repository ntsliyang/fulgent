package com.fulgent.aws.test;

import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.UploadResult;
import com.fulgent.aws.archive.ArchiveService;
import com.fulgent.aws.archive.ArchiveServiceImp;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;

public class ArchiveServiceTest {
    private static String accountId = "264556185871";  // hdong@fulgentgenetics.com
    private static ArchiveService serviceInst = new ArchiveServiceImp(accountId);

    @Test
    public void uploadArchive_test() {
        String vaultName = "drtest";
        String archiveDescription = "test.bam";
        String filePath = "./testData/test.bam";
        try {
            File f = new File(filePath);
            boolean exists = f.exists();
            if (! exists) {
                System.out.println("No file exists: " + filePath);
                return;
            }

            String archiveId = serviceInst.uploadArchive(vaultName, archiveDescription, filePath);
            System.out.println("archived successfully created!");
            System.out.println("archive ID: " + archiveId);
        }
        catch (Exception e)
        {
            System.out.println("Exception: " + e.toString());
        }
    }

    @Test
    public void deleteArchive_test() {
        String vaultName = "deleteVault_test";
        String archiveId = "archiveId";
        try {
            serviceInst.deleteArchive(archiveId, vaultName);
            System.out.println("archived successfully deleted!");
        } catch (Exception e) {
            System.out.println("Exception: " + e.toString());
        }
    }

    @Test
    public void downloadArchive_test() {
        String archiveId = "archiveId";
        String vaultName = "drtest";
        String filePath = "./testData/test.bam";
        try {
            serviceInst.downloadArchive(archiveId, vaultName, filePath);
        } catch (Exception e) {
            System.out.println("Exception: " + e.toString());
        }
    }

}
