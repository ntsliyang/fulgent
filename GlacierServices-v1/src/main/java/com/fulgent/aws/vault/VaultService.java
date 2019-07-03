package com.fulgent.aws.vault;

import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.model.CreateVaultResult;
import com.amazonaws.services.glacier.model.DescribeVaultOutput;
import com.amazonaws.services.glacier.model.ListVaultsResult;

import java.util.List;

public interface VaultService {
    public AmazonGlacier getVaultServiceImp(String accountId);

    public List<DescribeVaultOutput> listVaults();

    public CreateVaultResult createVault(String vaultName);

    public boolean deleteVault(String vaultName);
}
