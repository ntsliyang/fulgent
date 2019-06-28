import com.amazonaws.services.glacier.AmazonGlacier;
import com.amazonaws.services.glacier.AmazonGlacierClientBuilder;
import com.amazonaws.services.glacier.model.CreateVaultRequest;
import com.amazonaws.services.glacier.model.CreateVaultResult;

public class CreateVault {
    public static void main(String[] args) {

        String accountId;
        String vaultName;
        CreateVaultRequest request = null;

        AmazonGlacier glacierClient = AmazonGlacierClientBuilder.defaultClient();

        if (accountId != null) request = new CreateVaultRequest().withAccountId(accountId).withVaultName(vaultName);
        else request = new CreateVaultRequest().withVaultName(vaultName);

        CreateVaultResult result = glacierClient.createVault(request);
        System.out.println("vault successfully created!");
        System.out.println("vault location: " + result.getLocation());
    }
}
