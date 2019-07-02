package com.fulgent.aws.test;

import com.amazonaws.services.glacier.model.DescribeVaultOutput;
import com.fulgent.aws.vault.VaultServiceImp;
import org.apache.commons.cli.*;

import java.util.List;

public class ServiceTest {

    static void createVault_test(String accountId, String vaultName) {
    }

    static void listVaults_test(String accountId) {
        VaultServiceImp vii = new VaultServiceImp(accountId);

        List<DescribeVaultOutput> lv = vii.listVaults();
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

    public static void main(String[] args) {

        Options options = new Options();

        Option account = new Option("accountId", "account id", true, "account id");
        account.setRequired(true);
        options.addOption(account);

        Option vault = new Option("vaultName", "vault name ", true, "vault name");
        vault.setRequired(true);
        options.addOption(vault);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }

        createVault_test(cmd.getOptionValue("accountId"), cmd.getOptionValue("vaultName"));

        listVaults_test(cmd.getOptionValue("accountId"));
    }
}
