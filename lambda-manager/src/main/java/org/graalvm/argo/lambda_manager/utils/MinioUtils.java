package org.graalvm.argo.lambda_manager.utils;

import io.minio.BucketExistsArgs;
import io.minio.DownloadObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;

import static io.minio.MinioClient.builder;
import static java.lang.Integer.parseInt;
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.GROUP_WRITE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE;
import static java.util.Arrays.asList;
import static java.util.logging.Level.SEVERE;
import static org.graalvm.argo.lambda_manager.core.Environment.CODEBASE;


public class MinioUtils {

    public void downloadProfiles(String functionName) {
        try {
            ListObjectsArgs objectsArgs = ListObjectsArgs.builder().bucket(functionName).build();

            final File file = new File( CODEBASE + "/" + functionName + "/pgo-files");
            file.mkdir();
            HashSet<PosixFilePermission> permissions = new HashSet<>(asList(GROUP_READ, GROUP_WRITE, GROUP_EXECUTE,
                    OWNER_EXECUTE, OWNER_READ, OWNER_WRITE,
                    OTHERS_EXECUTE, OTHERS_READ, OTHERS_WRITE));

            Files.setPosixFilePermissions(file.toPath(), permissions);

            getClient()
                    .listObjects(objectsArgs)
                    .forEach(profile -> {
                        try {
                            final String filename = CODEBASE + "/" + functionName + "/pgo-files/" + profile.get().objectName();
                            getClient().downloadObject(DownloadObjectArgs
                                    .builder()
                                    .bucket(functionName)
                                    .object(profile.get().objectName())
                                    .filename(filename)
                                    .build());

                            Files.setPosixFilePermissions(new File(filename).toPath(), permissions);

                        } catch (Exception e) {
                            Logger.log(SEVERE, "Error: "+e);
                        }
                    });

        } catch (Exception e) {
            Logger.log(SEVERE, "Error when trying to download profile(s) of the function: "+functionName + e);
        }

    }

    public boolean containsAnyProfile(String functionName) {
       try {
           if (bucketExists(functionName)) {
               ListObjectsArgs objectsArgs = ListObjectsArgs.builder().bucket(functionName).build();
               return getClient()
                       .listObjects(objectsArgs)
                       .iterator()
                       .hasNext();
           }
           return false;
       } catch (Exception e) {
           Logger.log(SEVERE, "Error: "+e);
       }
        System.out.println("Bucket does not contain any files");
        return false;
    }

    private MinioClient getClient() {
        final PropertyReader propertyReader = new PropertyReader();
        final String minioUrl = propertyReader.getProperty("minio-url");
        final String minioPort = propertyReader.getProperty("minio-port");
        final String minioUser = propertyReader.getProperty("minio-user");
        final String minioPassword = propertyReader.getProperty("minio-password");

        return builder()
                .endpoint(minioUrl, parseInt(minioPort), false)
                .credentials(minioUser, minioPassword)
                .build();
    }

    private boolean bucketExists(String functionName) throws Exception {
        return getClient().bucketExists(BucketExistsArgs.builder().bucket(functionName).build());
    }

}
