import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.model.DeleteArchiveRequest;
import com.amazonaws.services.glacier.model.DeleteArchiveResult;
import org.apache.commons.cli.*;

public class DeleteArchive {

    public static void main(String[] args) {
        Options options = new Options();

        Option account = new Option("accountId", "account id", true, "account id");
        account.setRequired(true);
        options.addOption(account);

        Option archive = new Option("archiveId", "archive id", true, "archive id");
        archive.setRequired(true);
        options.addOption(archive);

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
        String archiveId = cmd.getOptionValue("archiveId");
        String vaultName = cmd.getOptionValue("vaultName");

        DeleteArchiveRequest request = null;
        AmazonGlacier glacierClient = AmazonGlacierClientBuilder.defaultClient();
        if (accountId != null) {
            request = new DeleteArchiveRequest().withAccountId(accountId).withArchiveId(archiveId).withVaultName(vaultName);
        } else {
            request = new DeleteArchiveRequest().withArchiveId(archiveId).withVaultName(vaultName);
        }

        DeleteArchiveResult result = glacierClient.deleteArchive(request);
        System.out.println("archive successfully deleted!");
    }
}
