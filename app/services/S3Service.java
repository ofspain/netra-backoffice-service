package services;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import utilities.FileUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import com.typesafe.config.Config;
import java.util.UUID;

@Singleton
public class S3Service {
    private final S3Client s3;
    private final String bucket;

    @Inject
    public S3Service(Config config) {
        this.bucket = config.getString("aws.s3.bucket");
        String region = config.getString("aws.s3.region");

        if (config.hasPath("aws.s3.accessKeyId") && config.hasPath("aws.s3.secretAccessKey")) {
            // Dev → use hardcoded keys
            this.s3 = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(
                            StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(
                                            config.getString("aws.s3.accessKeyId"),
                                            config.getString("aws.s3.secretAccessKey")
                                    )
                            )
                    )
                    .build();
        } else {
            // Prod → use ~/.aws/credentials or IAM role
            this.s3 = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
        }
    }

    public String uploadBase64(String base64, String folder) {
        byte[] data = FileUtils.decodeBase64(base64);
        if (data.length == 0) {
            throw new IllegalArgumentException("No file data provided.");
        }

        String mime = FileUtils.detectMimeType(data);
        String extension = FileUtils.extensionFromMime(mime);

        String key = folder + "/" + UUID.randomUUID() + "." + extension;

        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(mime)
                        .build(),
                RequestBody.fromBytes(data)
        );

        return key;
    }
}
