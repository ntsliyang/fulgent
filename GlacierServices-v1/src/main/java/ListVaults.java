import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.model.DescribeVaultOutput;
import com.amazonaws.services.glacier.model.ListVaultsRequest;
import com.amazonaws.services.glacier.model.ListVaultsResult;

import java.util.List;

public class ListVaults {
    public static void main(String[] args) {
        AmazonGlacier glacierClient = AmazonGlacierClientBuilder.defaultClient();
        String marker = null;
        do {
            ListVaultsRequest request = new ListVaultsRequest()
                    .withLimit("5")
                    .withMarker(marker);
            ListVaultsResult listVaultsResult = glacierClient.listVaults(request);

            List<DescribeVaultOutput> vaultList = listVaultsResult.getVaultList();
            marker = listVaultsResult.getMarker();
            for (DescribeVaultOutput vault : vaultList) {
                System.out.println(
                        "\nCreationDate: " + vault.getCreationDate() +
                                "\nLastInventoryDate: " + vault.getLastInventoryDate() +
                                "\nNumberOfArchives: " + vault.getNumberOfArchives() +
                                "\nSizeInBytes: " + vault.getSizeInBytes() +
                                "\nVaultARN: " + vault.getVaultARN() +
                                "\nVaultName: " + vault.getVaultName());
            }
        } while (marker != null);
    }
}
