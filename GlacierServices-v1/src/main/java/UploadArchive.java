import com.amazonaws.services.glacier.model.UploadArchiveResult;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManagerBuilder;
import com.amazonaws.services.glacier.transfer.UploadResult;
import org.apache.commons.cli.*;

import java.io.File;

public class UploadArchive {
    public static void main(String[] args) throws Exception {
        Options options = new Options();

        Option vault = new Option("vaultName", "vault name", true, "vault name");
        vault.setRequired(true);
        options.addOption(vault);

        Option archive = new Option("archiveDescription", "archive description", true, "archive description");
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

        String vaultName = cmd.getOptionValue("vaultName");
        String archiveDescription = cmd.getOptionValue("archiveDescription");
        String file = cmd.getOptionValue("file");

        ArchiveTransferManager atm = new ArchiveTransferManagerBuilder().build();
        System.out.println("atm created!");
        UploadResult result = atm.upload(vaultName, archiveDescription, new File(file));
        System.out.println("archived successfully created!");
        System.out.println("archive ID: " + result.getArchiveId());
    }
}
