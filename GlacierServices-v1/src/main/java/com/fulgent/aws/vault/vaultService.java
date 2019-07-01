package com.fulgent.aws.vault;

import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.model.DescribeVaultOutput;
import com.amazonaws.services.glacier.model.ListVaultsResult;

import java.util.List;

public interface vaultService {
    public AmazonGlacier getVaultServiceImp(String accountId);

    public List<DescribeVaultOutput> listVaults();

    public ListVaultsResult createVault();

    public boolean deleteVault();
}
