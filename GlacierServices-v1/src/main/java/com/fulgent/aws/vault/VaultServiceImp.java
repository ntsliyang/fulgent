package com.fulgent.aws.vault;

import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.model.*;

import java.util.ArrayList;
import java.util.List;

public class VaultServiceImp {
    AmazonGlacier glacierClient = null;
    CreateVaultRequest request = null;
    String accountId = "";

    private VaultServiceImp(String accountId)
    {
        glacierClient = AmazonGlacierClientBuilder.defaultClient();
        this.accountId = accountId;
    }

    public AmazonGlacier getVaultServiceImp(String accountId)
    {
        if (glacierClient == null) {
            glacierClient = AmazonGlacierClientBuilder.defaultClient();
        }

        return glacierClient;
    }

    public List<DescribeVaultOutput> listVaults()
    {
        List<DescribeVaultOutput> result = new ArrayList<>();
        String marker = null;
        do {
            ListVaultsRequest request = new ListVaultsRequest()
                    .withLimit("5")
                    .withMarker(marker);
            ListVaultsResult listVaultsResult = this.glacierClient.listVaults(request);

            List<DescribeVaultOutput> vaultList = listVaultsResult.getVaultList();
            result.addAll(vaultList);
            marker = listVaultsResult.getMarker();
            for (DescribeVaultOutput vault : vaultList) {
                System.out.println(
                        "\nCreationDate: " + vault.getCreationDate() +
                                "\nLastInventoryDate: " + vault.getLastInventoryDate() +
                                "\nNumberOfArchives: " + vault.getNumberOfArchives() +
                                "\nSizeInBytes: " + vault.getSizeInBytes() +
                                "\nVaultARN: " + vault.getVaultARN() +
                                "\nVaultName: " + vault.getVaultName());
            }
        } while (marker != null);

        return result;
    }

    public ListVaultsResult createVault(String vaultName)
    {
        if (accountId != null) request = new CreateVaultRequest().withAccountId(this.accountId).withVaultName(vaultName);
        else request = new CreateVaultRequest().withVaultName(vaultName);

        CreateVaultResult result = glacierClient.createVault(request);
        System.out.println("vault successfully created!");
        System.out.println("vault location: " + result.getLocation());
        return null;
    }

    public boolean deleteVault()
    {
        //Todo:
        return true;
    }
}
