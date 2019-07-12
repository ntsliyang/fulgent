# GlacierServices-v2

This repo includes two [AWS S3 Glacier](https://aws.amazon.com/glacier/) high-level API [ArchiveTransferManager](src/main/java/archive) and [VaultTransferManager](src/main/java/vault), which provide interfaces to enable different kinds of actions on **archive** and **vault**.

## Usage    
```bash
java -cp target/GlacierServices-v2-1.0-SNAPSHOT-jar-with-dependencies.jar Main [options] [parameters]
``` 

where options and corresponding parameters include:
- create a vault with specified `accountId` and `vaultName` (click [here](https://docs.aws.amazon.com/amazonglacier/latest/dev/creating-vaults-sdk-java.html) for v1 implementation)
```bash
    createVault [accountId] [vaultName]
``` 

- describe a vault with specified 'accountId' and 'vaultName' (click [here](https://docs.aws.amazon.com/amazonglacier/latest/dev/retrieving-vault-info-sdk-java.html) for v1 implementation)
```bash
    describeVault [accountId] [vaultName]
```    

- configure notifications (including `set`, `get`, and `delete`) (click [here](https://docs.aws.amazon.com/amazonglacier/latest/dev/configuring-notifications-sdk-java.html) for v1 implementation)
```bash
    getVaultNotifications [accountId] [vaultName]
    setVaultNotifications [vaultName]
    deleteVaultNotifications [accountId] [vaultName]
```

- download inventory list of a vault (click [here](https://docs.aws.amazon.com/amazonglacier/latest/dev/retrieving-vault-inventory-java.html) for v1 implementation)
```bash
    downloadInventory [vaultName] [fileName]
```    
   
- delete specified vault (click [here](https://docs.aws.amazon.com/amazonglacier/latest/dev/deleting-vaults-sdk-java.html) for v1 implementation)
```bash
    deleteVault [vaultName]
```
 
- upload the specified archive to specified vault (click [here](https://docs.aws.amazon.com/amazonglacier/latest/dev/uploading-an-archive-single-op-using-java.html) and [here](https://docs.aws.amazon.com/amazonglacier/latest/dev/uploading-archive-mpu.html) for v1 implementation) 
```bash
    uploadArchive [accountId] [vaultName] [archiveDescription] [fileName]
```    

- download the specified archive (click [here](https://docs.aws.amazon.com/amazonglacier/latest/dev/downloading-an-archive-using-java.html) for v1 implementation)   
```bash
    downloadArchive [accountId] [archiveId] [vaultName] [fileName]
```
    
- delete the specified archive (click [here](https://docs.aws.amazon.com/amazonglacier/latest/dev/deleting-an-archive-using-java.html) for v1 implementation)
```bash
    deleteArchive [accountId] [archiveId] [vaultName]
```
    
   
    

