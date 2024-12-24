package AWS.API.service.impl;

import AWS.API.DTO.ResponseMessage;
import AWS.API.UtilsMethods;
import AWS.API.entity.S3Bucket;
import AWS.API.entity.User;
import AWS.API.repository.S3BucketRepository;
import AWS.API.service.S3Service;
import AWS.API.service.SESService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;


import java.util.LinkedList;
import java.util.List;
import java.util.Objects;


@Service
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;
    private final Region region;
    private final UtilsMethods utilsMethods;
    private final S3BucketRepository s3BucketRepository;
    private final SESService sesService;



    public S3ServiceImpl(@Value("${aws.accessKeyId}") String accessKeyId,
                         @Value("${aws.secretAccessKey}") String secretAccessKey,
                         @Value("${aws.region}") String region,
                         UtilsMethods utilsMethods, S3BucketRepository s3BucketRepository, SESService sesService) {


        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        this.region = Region.of(region);

        this.s3Client = S3Client.builder()
                .region(this.region)
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
        this.s3BucketRepository = s3BucketRepository;
        this.utilsMethods = utilsMethods;
        this.sesService = sesService;

    }

    @Override
    public LinkedList<String> getBucketList(String authorization) {

        User user = utilsMethods.extractUsernameFromAuthorizationHeader(authorization);
        LinkedList<String> bucketsInfo = new LinkedList<>();
        List<S3Bucket> buckets = s3BucketRepository.findAll().stream().filter(bucket -> Objects.equals(bucket.getUser().getId(), user.getId())).toList();

        for(S3Bucket bucket: buckets){
            bucketsInfo.add(bucket.getBucketIdentifier());
        }
        return bucketsInfo;
    }
    private void deleteAllObjects(String bucketName) {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucketName).build();
        ListObjectsV2Response listObjectsV2Response;

        // Step 1: List and delete all objects in the bucket
        do {
            listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
            List<S3Object> objects = listObjectsV2Response.contents();

            for (S3Object object : objects) {
                String objectKey = object.key();
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .build());
            }

            // Set the continuation token if the response is truncated
            listObjectsV2Request = listObjectsV2Request.toBuilder()
                    .continuationToken(listObjectsV2Response.nextContinuationToken())
                    .build();

        } while (listObjectsV2Response.isTruncated());

    }

    @Override
    public ResponseMessage createBucket(String bucketName, String authorization) {
        User user = utilsMethods.extractUsernameFromAuthorizationHeader(authorization);
        S3Bucket foundS3Bucket = s3BucketRepository.findByBucketIdentifier(bucketName);
        if(foundS3Bucket != null) throw new EntityExistsException("Can not create multiple buckets with the same identifier");

        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

        CreateBucketResponse createBucketResponse = s3Client.createBucket(createBucketRequest);

        PublicAccessBlockConfiguration publicAccessBlockConfig = PublicAccessBlockConfiguration.builder()
                .blockPublicAcls(false)
                .ignorePublicAcls(false)
                .blockPublicPolicy(false)
                .restrictPublicBuckets(false)
                .build();

        PutPublicAccessBlockRequest putPublicAccessBlockRequest = PutPublicAccessBlockRequest.builder()
                .bucket(bucketName)
                .publicAccessBlockConfiguration(publicAccessBlockConfig)
                .build();

        s3Client.putPublicAccessBlock(putPublicAccessBlockRequest);

        WebsiteConfiguration websiteConfiguration = WebsiteConfiguration.builder()
                .indexDocument(IndexDocument.builder()
                        .suffix("index.html")
                        .build())
                .build();

        PutBucketWebsiteRequest putBucketWebsiteRequest = PutBucketWebsiteRequest.builder()
                .bucket(bucketName)
                .websiteConfiguration(websiteConfiguration)
                .build();

        s3Client.putBucketWebsite(putBucketWebsiteRequest);

        String bucketPolicy = "{\n" +
                "    \"Version\": \"2012-10-17\",\n" +
                "    \"Statement\": [\n" +
                "        {\n" +
                "            \"Sid\": \"PublicReadGetObject\",\n" +
                "            \"Effect\": \"Allow\",\n" +
                "            \"Principal\": \"*\",\n" +
                "            \"Action\": \"s3:GetObject\",\n" +
                "            \"Resource\": \"arn:aws:s3:::" + bucketName + "/*\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"Sid\": \"PublicPutObject\",\n" +
                "            \"Effect\": \"Allow\",\n" +
                "            \"Principal\": \"*\",\n" +
                "            \"Action\": \"s3:PutObject\",\n" +
                "            \"Resource\": \"arn:aws:s3:::" + bucketName + "/*\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";

        PutBucketPolicyRequest putBucketPolicyRequest = PutBucketPolicyRequest.builder()
                .bucket(bucketName)
                .policy(bucketPolicy)
                .build();

        s3Client.putBucketPolicy(putBucketPolicyRequest);


        S3Bucket s3Bucket = S3Bucket.builder()
                .bucketIdentifier(bucketName)
                .user(user)
                .build();
        s3BucketRepository.save(s3Bucket);

        sesService.sendEmail("arenasportcenter2000@gmail.com", user.getUsername(),"S3 Bucket", "Your bucket" + bucketName + "was created successfully");

        return new ResponseMessage("Bucket " + bucketName + " was created successfully.");
    }

    @Override
    public ResponseMessage deleteBucket(String bucketName, String authorization) {
        User user = utilsMethods.extractUsernameFromAuthorizationHeader(authorization);
        S3Bucket bucket = s3BucketRepository.findByBucketIdentifier(bucketName);
        if(bucket == null) throw new EntityNotFoundException("There is no bucket with this name");
        if(!bucket.getUser().getId().equals(user.getId())) throw new EntityNotFoundException("User is not allowed to delete this bucket");

        deleteAllObjects(bucketName);

        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucketName).build();
        s3Client.deleteBucket(deleteBucketRequest);
        s3BucketRepository.delete(bucket);
        return new ResponseMessage("The bucket " + bucketName + " is deleting.");
    }


}
