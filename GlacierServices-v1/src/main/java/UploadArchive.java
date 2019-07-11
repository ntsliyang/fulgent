import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.glacier.model.UploadArchiveResult;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManagerBuilder;
import com.amazonaws.services.glacier.transfer.UploadResult;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
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

        File f = new File(file);

        AmazonIdentityManagement iamClient = AmazonIdentityManagementClient.builder().build();
        ProgressListener listener = new ProgressListener() {
            long len = f.length();
            long sent = 0;
            int lastPct = 0;

            @Override
            public void progressChanged(ProgressEvent progressEvent) {
                // System.out.println(progressEvent.toString());
                sent += progressEvent.getBytesTransferred();
                int l_pct = (int)((double)sent / (double)len * 100.0);
                if (l_pct != lastPct) {
                    lastPct = l_pct;
                    System.out.println("" + l_pct + "%");
                }
            }
        };

        ArchiveTransferManager atm = new ArchiveTransferManagerBuilder().build();
        System.out.println("atm created!");
        UploadResult result = atm.upload(iamClient.getUser().getUser().getUserId(), vaultName, archiveDescription, f, listener);
        System.out.println("archived successfully created!");
        System.out.println("archive ID: " + result.getArchiveId());
    }
}
