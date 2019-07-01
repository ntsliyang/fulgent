import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.model.CreateVaultRequest;
import com.amazonaws.services.glacier.model.CreateVaultResult;
import org.apache.commons.cli.*;

public class CreateVault {
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

        String accountId = cmd.getOptionValue("accountId");
        String vaultName = cmd.getOptionValue("vaultName");

        CreateVaultRequest request = null;

        AmazonGlacier glacierClient = AmazonGlacierClientBuilder.defaultClient();

        if (accountId != null) request = new CreateVaultRequest().withAccountId(accountId).withVaultName(vaultName);
        else request = new CreateVaultRequest().withVaultName(vaultName);

        CreateVaultResult result = glacierClient.createVault(request);
        System.out.println("vault successfully created!");
        System.out.println("vault location: " + result.getLocation());
    }
}
