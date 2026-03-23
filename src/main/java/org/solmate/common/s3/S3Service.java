package org.solmate.common.s3;

import java.io.IOException;
import java.util.UUID;

import org.solmate.common.exception.GeneralException;
import org.solmate.common.status.ErrorStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /** S3에 파일을 업로드하고 접근 URL을 반환 */
    public String uploadFile(MultipartFile file, String directory) {
        String key = buildKey(directory, file.getOriginalFilename());
        PutObjectRequest request = buildPutRequest(key, file);

        try {
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new GeneralException(ErrorStatus.S3_FILE_UPLOAD_FAILED);
        }

        return buildFileUrl(key);
    }

    /** S3에서 파일을 삭제. 이미지 변경 시 기존 파일 정리에 사용 */
    public void deleteFile(String fileUrl) {
        String key = extractKey(fileUrl);

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3Client.deleteObject(request);
    }

    private String buildKey(String directory, String originalFilename) {
        return directory + "/" + UUID.randomUUID() + "_" + originalFilename;
    }

    private PutObjectRequest buildPutRequest(String key, MultipartFile file) {
        return PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();
    }

    public String buildFileUrl(String key) {
        return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + key;
    }

    private String extractKey(String fileUrl) {
        return fileUrl.substring(fileUrl.indexOf(".com/") + 5);
    }
}
