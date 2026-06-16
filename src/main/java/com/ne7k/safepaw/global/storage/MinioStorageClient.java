package com.ne7k.safepaw.global.storage;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

@Component
public class MinioStorageClient {

    private final StorageProperties props;
    private final S3Presigner presigner;

    public MinioStorageClient(StorageProperties props) {
        this.props = props;

        // minio 로그인 정보
        var creds = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.accessKey(), props.secretKey())
        );

        // S3presigner 빌드
        this.presigner = S3Presigner.builder()
                .region(Region.of(props.region()))
                .endpointOverride(URI.create(props.publicEndpoint()))
                .credentialsProvider(creds)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(props.pathStyleAccess())
                        .checksumValidationEnabled(false)
                        .build()
                )
                .build();
    }

    // 업로드용 일회성 url 발급
    public String presignPut(String bucket, String key, String contentType, Duration ttl) {
        var put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();
        var presign = PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(put)
                .build();
        return presigner.presignPutObject(presign).url().toString();
    }

    // 조회용 url 조립
    public String publicUrl(String bucket, String key) {
        if (key == null) return null;
        String base = props.publicBaseUrl().replaceAll("/$", ""); // trailing slash 제거
        return base + "/" + bucket + "/" + key;
    }
}
