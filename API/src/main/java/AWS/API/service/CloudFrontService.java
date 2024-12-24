package AWS.API.service;

import AWS.API.DTO.DistributionData;
import AWS.API.DTO.ResponseMessage;
import AWS.API.entity.User;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.util.List;

public interface CloudFrontService {

    String createDistribution(S3Client s3Client, String bucketName, User user);

    List<DistributionData> getDistributions(String authorization);

    void deleteDistribution(String authorization, String distributionName) throws InterruptedException;

    ResponseMessage disableOrEnable(String authorization, String distributionIdentifier, boolean command);

    void uploadDirectoryToS3Bucket(MultipartFile zipFile, String targetBucket, String authorization) throws IOException;


}
