import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManagerBuilder;
import org.apache.commons.cli.*;

import java.io.File;

public class DownloadArchive {
    public static void main(String[] args) {
        Options options = new Options();

        Option account = new Option("accountId", "account ID", true, "account ID");
        account.setRequired(false);
        options.addOption(account);

        Option vault = new Option("vaultName", "vault name", true, "vaule name");
        vault.setRequired(true);
        options.addOption(vault);

        Option archive = new Option("archiveId", "archive ID", true, "archive ID");
        archive.setRequired(true);
        options.addOption(archive);

        Option path = new Option("file", "file path", true, "file path");
        path.setRequired(true);
        options.addOption(path);

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
        String archiveId = cmd.getOptionValue("archiveId");
        String file = cmd.getOptionValue("file");

        System.out.println(file);

        ArchiveTransferManager atm = new ArchiveTransferManagerBuilder().build();
        System.out.println("atm created!");
        if (accountId != null) atm.download(accountId, vaultName, archiveId, new File(file));
        else atm.download(vaultName, archiveId, new File(file));
        System.out.println("file successfully downloaded!");
    }
}
