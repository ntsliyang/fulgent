package com.fulgent.aws.vault;

import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.model.*;

import java.util.ArrayList;
import java.util.List;

public class VaultServiceImp implements VaultService{
    AmazonGlacier glacierClient = null;
    CreateVaultRequest cRequest = null;
    DeleteVaultRequest dRequest = null;
    String accountId = "";

    public VaultServiceImp(String accountId)
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
        } while (marker != null);

        return result;
    }

    public String createVault(String vaultName)
    {
        if (accountId != null) cRequest = new CreateVaultRequest().withAccountId(this.accountId).withVaultName(vaultName);
        else cRequest = new CreateVaultRequest().withVaultName(vaultName);

        CreateVaultResult result = glacierClient.createVault(cRequest);
//        System.out.println("vault successfully created!");
//        System.out.println("vault location: " + result.getLocation());
        return result.getLocation();
    }

    public void deleteVault(String vaultName)
    {
        //Todo:
        if (accountId != null) dRequest = new DeleteVaultRequest().withAccountId(this.accountId).withVaultName(vaultName);
        else dRequest = new DeleteVaultRequest().withVaultName(vaultName);

        glacierClient.deleteVault(dRequest);
    }
}
