package com.fulgent.aws.test;


import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.model.*;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.fulgent.aws.vault.VaultService;
import com.fulgent.aws.vault.VaultServiceImp;

public class VaultServiceTest {
    private static String accountId = "264556185871";  // hdong@fulgentgenetics.com
    private static VaultService vaultServiceInst = new VaultServiceImp(accountId);

    @Test
    public void createVault_test() {
        String vaultName = "createVault_test";
        String result = vaultServiceInst.createVault(vaultName);
        System.out.println("vault successfully created!");
        System.out.println("vault location: " + result);
    }

    @Test
    public void listVaults_test() {
        List<DescribeVaultOutput> lv = vaultServiceInst.listVaults();
        for (DescribeVaultOutput v : lv) {
            System.out.println(
                    "\nCreationDate: " + v.getCreationDate() +
                            "\nLastInventoryDate: " + v.getLastInventoryDate() +
                            "\nNumberOfArchives: " + v.getNumberOfArchives() +
                            "\nSizeInBytes: " + v.getSizeInBytes() +
                            "\nVaultARN: " + v.getVaultARN() +
                            "\nVaultName: " + v.getVaultName());
        }
    }

    @Test
    public void downloadVault_test() {
        String vaultName = "createVault_test";
        vaultServiceInst.downloadVault(vaultName);
        System.out.print("downloadVault_test done, inventory is at ???");
    }
}
