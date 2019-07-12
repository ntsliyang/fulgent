package archive;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.io.ResettableInputStream;
import software.amazon.awssdk.utils.BinaryUtils;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class TreeHashGenerator {
    /**
     * Calculates a hex encoded binary hash using a tree hashing algorithm for
     * the data in the specified file.
     *
     * @param file
     *            The file containing the data to hash.
     *
     * @return The hex encoded binary tree hash for the data in the specified
     *         file.
     *
     * @throws SdkClientException
     *             If any problems were encountered reading the data or
     *             computing the hash.
     */
    public static String calculateTreeHash(File file)
            throws SdkClientException {
        ResettableInputStream is = null;
        try {
            is = ResettableInputStream.newResettableInputStream(file);
            return calculateTreeHash(is);
        } catch (Exception e) {
            throw SdkClientException.create("Unable to compute hash for file: "
                    + file.getAbsolutePath(), e);
        }
         finally {
            if (is != null)
                is.release();
        }
    }

    /**
     * Calculates a hex encoded binary hash using a tree hashing algorithm for
     * the data in the specified input stream. The method will consume all the
     * inputStream and close it when returned.
     *
     * @param input
     *            The input stream containing the data to hash.
     *
     * @return The hex encoded binary tree hash for the data in the specified
     *         input stream.
     *
     * @throws SdkClientException
     *             If problems were encountered reading the data or calculating
     *             the hash.
     */
    public static String calculateTreeHash(InputStream input)
            throws SdkClientException {
        try {
            TreeHashInputStream treeHashInputStream =
                    new TreeHashInputStream(input);
            byte[] buffer = new byte[1024];
            while (treeHashInputStream.read(buffer, 0, buffer.length) != -1);
            // closing is currently required to compute the checksum
            treeHashInputStream.close();
            return calculateTreeHash(treeHashInputStream.getChecksums());
        } catch (Exception e) {
            throw SdkClientException.create("Unable to compute hash", e);
        }
    }

    /**
     * Returns the hex encoded binary tree hash for the individual checksums
     * given. The sums are assumed to have been generated from sequential 1MB
     * portions of a larger file, with the possible exception of the last part,
     * which may be less than a full MB.
     *
     * @return The combined hex encoded binary tree hash for the individual
     *         checksums specified.
     *
     * @throws SdkClientException
     *             If problems were encountered reading the data or calculating
     *             the hash.
     */
    public static String calculateTreeHash(List<byte[]> checksums) throws SdkClientException {

        /*
         * The tree hash algorithm involves concatenating adjacent pairs of
         * individual checksums, then taking the checksum of the resulting bytes
         * and storing it, then recursing on this new list until there is only
         * one element. Any final odd-numbered parts at each step are carried
         * over to the next iteration as-is.
         */
        List<byte[]> hashes = new ArrayList<byte[]>();
        hashes.addAll(checksums);
        while ( hashes.size() > 1 ) {
            List<byte[]> treeHashes = new ArrayList<byte[]>();
            for ( int i = 0; i < hashes.size() / 2; i++ ) {
                byte[] firstPart = hashes.get(2 * i);
                byte[] secondPart = hashes.get(2 * i + 1);
                byte[] concatenation = new byte[firstPart.length + secondPart.length];
                System.arraycopy(firstPart, 0, concatenation, 0, firstPart.length);
                System.arraycopy(secondPart, 0, concatenation, firstPart.length, secondPart.length);
                try {
                    treeHashes.add(computeSHA256Hash(concatenation));
                } catch (Exception e) {
                    throw SdkClientException.create("Unable to compute hash", e);
                }
            }
            if ( hashes.size() % 2 == 1 ) {
                treeHashes.add(hashes.get(hashes.size() - 1));
            }
            hashes = treeHashes;
        }

        return BinaryUtils.toHex(hashes.get(0));
    }

    private static byte[] computeSHA256Hash(byte[] data) throws NoSuchAlgorithmException, IOException {
        BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data));
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[16384];
            int bytesRead = -1;
            while ( (bytesRead = bis.read(buffer, 0, buffer.length)) != -1 ) {
                messageDigest.update(buffer, 0, bytesRead);
            }
            return messageDigest.digest();
        } finally {
            try { bis.close(); } catch ( Exception e ) {}
        }
    }
}
