import archive.ArchiveTransferManager;
import software.amazon.awssdk.services.glacier.GlacierClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import vault.VaultTransferManager;

public class Main {

    public static void main(String[] args) throws Exception {

        VaultTransferManager vtm = VaultTransferManager.builder()
                .glacierClient(GlacierClient.create())
                .sqsClient(SqsClient.create())
                .snsClient(SnsClient.create())
                .build();

        ArchiveTransferManager atm = ArchiveTransferManager.builder()
                .glacierClient(GlacierClient.create())
                .sqsClient(SqsClient.create())
                .snsClient(SnsClient.create())
                .build();

        String operation = args[0];

        if (operation.equals("createVault")) {
            String accountId = args[1];
            String vaultName = args[2];
            vtm.create(accountId, vaultName);
        } else if (operation.equals("describeVault")) {
            String accountId = args[1];
            String vaultName = args[2];
            vtm.describe(accountId, vaultName);
        } else if (operation.equals("listVaults")) {
            vtm.list();
        } else if (operation.equals("getVaultNotifications")) {
            String accountId = args[1];
            String vaultName = args[2];
            vtm.getNotifications(accountId, vaultName);
        } else if (operation.equals("setVaultNotifications")) {
            String vaultName = args[1];
            vtm.setNotifications(vaultName);
        } else if (operation.equals("deleteVaultNotifications")) {
            String accountId = args[1];
            String vaultName = args[2];
            vtm.deleteNotifications(accountId, vaultName);
        } else if (operation.equals("downloadInventory")) {
            String vaultName = args[1];
            String fileName = args[2];
            vtm.downloadInventory(vaultName, fileName);
        } else if (operation.equals("deleteVault")) {
            String vaultName = args[1];
            vtm.delete(vaultName);
        } else if (operation.equals("uploadArchive")) {
            String accountId = args[1];
            String vaultName = args[2];
            String archiveDescription = args[3];
            String fileName = args[4];
            if (accountId != null) atm.upload(accountId, vaultName, archiveDescription, fileName);
            else atm.upload(vaultName, archiveDescription, fileName);
        } else if (operation.equals("downloadArchive")) {
            String accountId = args[1];
            String archiveId = args[2];
            String vaultName = args[3];
            String fileName = args[4];
            if (accountId != null) atm.download(accountId, vaultName, archiveId, fileName);
            else atm.download(vaultName, archiveId, fileName);
        } else if (operation.equals("deleteArchive")) {
            String accountId = args[1];
            String archiveId = args[2];
            String vaultName = args[3];
            if (accountId != null) atm.delete(accountId, vaultName, archiveId);
            else atm.delete(vaultName, archiveId);
        }
    }
}
