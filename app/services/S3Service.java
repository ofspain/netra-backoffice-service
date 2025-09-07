package services;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
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

        String profile = config.hasPath("aws.profile") ? config.getString("aws.profile") : null;
        String region = config.getString("aws.region"); // required
        this.bucket = config.getString("aws.s3.bucket"); // required

        if (profile != null && !profile.isEmpty()) {
            this.s3 = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(ProfileCredentialsProvider.create(profile))
                    .build();
        } else {
            this.s3 = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
        }

    }

    public String uploadBase64(String base64, String folder) {
        byte[] data = FileUtils.decodeBase64(base64);
        System.out.println("uploading data lenght "+data.length);
        if (data.length == 0) {
            throw new IllegalArgumentException("No file data provided.");
        }

        String mime = FileUtils.detectMimeType(data);
        String extension = FileUtils.extensionFromMime(mime);

        String key = folder + "/" + UUID.randomUUID() + "." + extension;

        System.out.println("path.... "+key);

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

    public byte[] getObject(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(request);
            return objectBytes.asByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch S3 object with key: " + key, e);
        }
    }

}
