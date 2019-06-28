import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManagerBuilder;

import java.io.File;

public class Main {

    private static AmazonGlacier glacierClient = AmazonGlacierClient.builder().build();
    private static String accountId = "410351524560";
    private static String vaultName = "example-vault";
    private static String archiveId = "q45i9Xiuw0FxbTuU8_1CQWgNjdjjR6JwAY7tYMLd4xQJoyEwLpxoGSyqeCoJnhciL8zK4xMO0yYmEhBZNrHcU0KKB5u1fzry8nyYsDW1eI__8hOW93NroxsxCipAibz8AQyiEN7IGw";
    private static String archivePath = "/Users/apple/Downloads/download.jpg";

    public static void main(String[] args) {
	// write your code here
        ArchiveTransferManager atm = new ArchiveTransferManagerBuilder().build();
        System.out.println("atm created!");
        atm.download(accountId, vaultName, archiveId, new File(archivePath));
        System.out.println("file successfully downloaded!");
    }
}
