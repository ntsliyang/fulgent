package com.fulgent.aws.vault;

import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.model.DescribeVaultOutput;

import java.util.List;

public interface VaultService {
    AmazonGlacier getVaultServiceImp(String accountId);

    List<DescribeVaultOutput> listVaults(); // return names of vaults

    String createVault(String vaultName); // return location of newly created vault

    void deleteVault(String vaultName);
}
