package org.esa.snap.vfs.remote.s3;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;
import org.esa.snap.vfs.remote.ObjectStorageWalker;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File System Service Provider for S3 Object Storage VFS.
 *
 * @author Norman Fomferra
 * @author Adrian Drăghici
 */
public class S3FileSystemProvider extends AbstractRemoteFileSystemProvider {

    /**
     * The name of access key ID property, used on S3 VFS instance creation parameters and defining remote file repository properties.
     */
    private static final String ACCESS_KEY_ID_PROPERTY_NAME = "accessKeyId";

    /**
     * The name of secret access key property, used on S3 VFS instance creation parameters and defining remote file repository properties.
     */
    private static final String SECRET_ACCESS_KEY_PROPERTY_NAME = "secretAccessKey";

    /**
     * The default value of root property, used on VFS instance creation parameters.
     */
    private static final String S3_ROOT = "S3:/";

    /**
     * The value of S3 provider scheme.
     */
    private static final String SCHEME = "s3";

    private static Logger logger = Logger.getLogger(S3FileSystemProvider.class.getName());

    private String bucketAddress = "";
    private String accessKeyId = "";
    private String secretAccessKey = "";
    private String delimiter = DELIMITER_PROPERTY_DEFAULT_VALUE;

    public S3FileSystemProvider() {
        super(S3_ROOT);
    }

    /**
     * Save connection data on this provider.
     *
     * @param bucketAddress   The bucket Address
     * @param accessKeyId     The access key id S3 credential (username)
     * @param secretAccessKey The secret access key S3 credential (password)
     */
    private void setupConnectionData(String bucketAddress, String accessKeyId, String secretAccessKey) {
        this.bucketAddress = bucketAddress != null ? bucketAddress : "";
        this.accessKeyId = accessKeyId != null ? accessKeyId : "";
        this.secretAccessKey = secretAccessKey != null ? secretAccessKey : "";
    }

    public void setConnectionData(String serviceAddress, Map<String, ?> connectionData) {
        String newRoot = (String) connectionData.get(ROOT_PROPERTY_NAME);
        root = newRoot != null && !newRoot.isEmpty() ? newRoot : S3_ROOT;
        String newAccessKeyId = (String) connectionData.get(ACCESS_KEY_ID_PROPERTY_NAME);
        String newSecretAccessKey = (String) connectionData.get(SECRET_ACCESS_KEY_PROPERTY_NAME);
        setupConnectionData(serviceAddress, newAccessKeyId, newSecretAccessKey);
    }

    /**
     * Creates the VFS instance using this provider.
     *
     * @param address The VFS service address
     * @param env     The VFS parameters
     * @return The new VFS instance
     * @throws IOException If an I/O error occurs
     */
    @Override
    protected S3FileSystem newFileSystem(String address, Map<String, ?> env) throws IOException {
        return new S3FileSystem(this, address, delimiter);
    }

    /**
     * Creates the walker instance used by VFS provider to traverse VFS tree.
     *
     * @return The new VFS walker instance
     */
    @Override
    protected ObjectStorageWalker newObjectStorageWalker() {
        try {
            return new S3Walker(bucketAddress, accessKeyId, secretAccessKey, delimiter, root);
        } catch (ParserConfigurationException | SAXException ex) {
            logger.log(Level.SEVERE, "Unable to create walker instance used by S3 VFS to traverse tree. Details: " + ex.getMessage());
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Gets the service address of this VFS provider.
     *
     * @return The VFS service address
     */
    public String getProviderAddress() {
        return bucketAddress;
    }

    /**
     * Gets the connection channel for this VFS provider.
     *
     * @param url               The URL address to connect
     * @param method            The HTTP method (GET POST DELETE etc)
     * @param requestProperties The properties used on the connection
     * @return The connection channel
     * @throws IOException If an I/O error occurs
     */
    public URLConnection getProviderConnectionChannel(URL url, String method, Map<String, String> requestProperties) throws IOException {
        return S3ResponseHandler.getConnectionChannel(url, method, requestProperties, accessKeyId, secretAccessKey);
    }

    /**
     * Gets the URI scheme that identifies this VFS provider.
     *
     * @return The URI scheme
     */
    @Override
    public String getScheme() {
        return SCHEME;
    }
}
