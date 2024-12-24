package AWS.API.service.impl;

import AWS.API.DTO.DistributionData;
import AWS.API.DTO.ResponseMessage;
import AWS.API.UtilsMethods;
import AWS.API.entity.S3Bucket;
import AWS.API.entity.User;
import AWS.API.repository.CloudFrontRepository;
import AWS.API.repository.S3BucketRepository;
import AWS.API.service.CloudFrontService;
import AWS.API.service.SESService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.*;
import software.amazon.awssdk.services.cloudfront.waiters.CloudFrontWaiter;
import software.amazon.awssdk.services.codebuild.CodeBuildClient;
import software.amazon.awssdk.services.codebuild.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CloudFrontServiceImpl implements CloudFrontService {

    private final CloudFrontClient cloudFrontClient;
    private final Region region;
    private final CloudFrontRepository cloudFrontRepository;
    private final UtilsMethods utilsMethods;
    private final S3BucketRepository s3BucketRepository;
    private final S3Client s3Client;
    private final CodeBuildClient codeBuildClient;
    private final SESService sesService;


    public CloudFrontServiceImpl(@Value("${aws.accessKeyId}") String accessKeyId,
                                 @Value("${aws.secretAccessKey}") String secretAccessKey,
                                 @Value("${aws.region}") String region, CloudFrontRepository cloudFrontRepository, UtilsMethods utilsMethods, S3BucketRepository s3BucketRepository, SESService sesService) {


        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        this.cloudFrontRepository = cloudFrontRepository;
        this.region = Region.of(region);
        this.utilsMethods = utilsMethods;
        this.s3BucketRepository = s3BucketRepository;
        this.cloudFrontClient = CloudFrontClient.builder()
                .region(this.region)
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
        this.s3Client = S3Client.builder()
                .region(this.region)
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
        this.codeBuildClient = CodeBuildClient.builder()
                .region(this.region)
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
        this.sesService = sesService;
    }

    @Override
    public String createDistribution(S3Client s3Client, String bucketName, User user) {

        final String region = s3Client.headBucket(b -> b.bucket(bucketName)).sdkHttpResponse().headers()
                .get("x-amz-bucket-region").get(0);
        final String originDomain = bucketName + ".s3." + region + ".amazonaws.com";
        String originId = originDomain; // Use the originDomain value for the originId.

        Origin origin = Origin.builder()
                .domainName(originDomain)
                .id(originId)
                .s3OriginConfig(builder -> builder.originAccessIdentity(""))
                .build();

        Origins origins = Origins.builder()
                .items(origin)
                .quantity(1)
                .build();

        DefaultCacheBehavior defaultCacheBehavior = DefaultCacheBehavior.builder()
                .allowedMethods(AllowedMethods.builder().items(Method.GET, Method.POST, Method.PUT, Method.DELETE, Method.HEAD, Method.PATCH, Method.OPTIONS).quantity(7).build())
                .targetOriginId(originId)
                .viewerProtocolPolicy(ViewerProtocolPolicy.ALLOW_ALL)
                .minTTL(200L)
                .forwardedValues(b5 -> b5.cookies(cp -> cp
                                .forward(ItemSelection.NONE))
                        .queryString(true))
                .build();

        CustomErrorResponse customErrorResponse403 = CustomErrorResponse.builder()
                .errorCode(403)
                .responsePagePath("/index.html")
                .responseCode("200")
                .errorCachingMinTTL(10L)
                .build();

        CustomErrorResponse customErrorResponse404 = CustomErrorResponse.builder()
                .errorCode(404)
                .responsePagePath("/index.html")
                .responseCode("200")
                .errorCachingMinTTL(10L)
                .build();

        CustomErrorResponses customErrorResponses = CustomErrorResponses.builder()
                .items(customErrorResponse403, customErrorResponse404)
                .quantity(2)
                .build();

        DistributionConfig distributionConfig = DistributionConfig.builder()
                .enabled(true)
                .defaultCacheBehavior(defaultCacheBehavior)
                .origins(origins)
                .comment("Distribution built with Java")
                .httpVersion(HttpVersion.HTTP2)
                .callerReference(Instant.now().toString())
                .customErrorResponses(customErrorResponses)
                .build();

        CreateDistributionRequest request = CreateDistributionRequest.builder()
                .distributionConfig(distributionConfig)
                .build();

        CreateDistributionResponse response = this.cloudFrontClient.createDistribution(request);

        final Distribution distribution = response.distribution();
        try (CloudFrontWaiter cfWaiter = CloudFrontWaiter.builder().client(cloudFrontClient).build()) {
            ResponseOrException<GetDistributionResponse> responseOrException = cfWaiter
                    .waitUntilDistributionDeployed(builder -> builder.id(distribution.id()))
                    .matched();
            responseOrException.response()
                    .orElseThrow(() -> new RuntimeException("Distribution not created"));
        }

        AWS.API.entity.Distribution cloudFrontDistribution = AWS.API.entity.Distribution.builder()
                .distributionIdentifier(distribution.id())
                .user(user)
                .build();

        this.cloudFrontRepository.save(cloudFrontDistribution);

        return distribution.domainName();
    }

    @Override
    public List<DistributionData> getDistributions(String authorization) {
        User user = utilsMethods.extractUsernameFromAuthorizationHeader(authorization);
        List<AWS.API.entity.Distribution> distributions = cloudFrontRepository.findAllByUserId(user.getId());

        List<DistributionData> result = new ArrayList<>();
        for (AWS.API.entity.Distribution dist : distributions) {
            try {
                GetDistributionRequest request = GetDistributionRequest.builder()
                        .id(dist.getDistributionIdentifier())
                        .build();

                GetDistributionResponse response = cloudFrontClient.getDistribution(request);
                String bucketName = "";
                if (response.distribution().distributionConfig().origins().items().get(0).domainName().endsWith("s3.us-east-1.amazonaws.com")) {
                        bucketName = response.distribution().distributionConfig().origins().items().get(0).domainName().replace(".s3.us-east-1.amazonaws.com", "");
                }

                DistributionData distributionInfo = new DistributionData(
                        response.distribution().id(),
                        response.distribution().domainName(),
                        bucketName,
                        response.distribution().status(),
                        response.distribution().distributionConfig().enabled()
                );
                result.add(distributionInfo);
            } catch (CloudFrontException e) {
                System.err.println("Error fetching distribution status: " + e.awsErrorDetails().errorMessage());
            }
        }
        return result;
    }

    @Override
    public void deleteDistribution(String authorization, String distributionIdentifier) {
        User user = utilsMethods.extractUsernameFromAuthorizationHeader(authorization);

        AWS.API.entity.Distribution distribution = cloudFrontRepository.findByDistributionIdentifier(distributionIdentifier);
        if (distribution == null) throw new EntityNotFoundException("No distribution was found");

        if(!distribution.getUser().getId().equals(user.getId())) throw new EntityNotFoundException("This user can not access this distribution");

        GetDistributionResponse response = cloudFrontClient.getDistribution(b -> b
                .id(distributionIdentifier));
        String tag = response.eTag();
        DistributionConfig distConfig = response.distribution().distributionConfig();

        cloudFrontClient.updateDistribution(builder -> builder
                .id(distributionIdentifier)
                .distributionConfig(builder1 -> builder1
                        .cacheBehaviors(distConfig.cacheBehaviors())
                        .defaultCacheBehavior(distConfig.defaultCacheBehavior())
                        .enabled(false)
                        .origins(distConfig.origins())
                        .comment(distConfig.comment())
                        .callerReference(distConfig.callerReference())
                        .defaultCacheBehavior(distConfig.defaultCacheBehavior())
                        .priceClass(distConfig.priceClass())
                        .aliases(distConfig.aliases())
                        .logging(distConfig.logging())
                        .defaultRootObject(distConfig.defaultRootObject())
                        .customErrorResponses(distConfig.customErrorResponses())
                        .httpVersion(distConfig.httpVersion())
                        .isIPV6Enabled(distConfig.isIPV6Enabled())
                        .restrictions(distConfig.restrictions())
                        .viewerCertificate(distConfig.viewerCertificate())
                        .webACLId(distConfig.webACLId())
                        .originGroups(distConfig.originGroups()))
                .ifMatch(tag));

        GetDistributionResponse distributionResponse;
        try (CloudFrontWaiter cfWaiter = CloudFrontWaiter.builder().client(cloudFrontClient).build()) {
            ResponseOrException<GetDistributionResponse> responseOrException = cfWaiter
                    .waitUntilDistributionDeployed(builder -> builder.id(distributionIdentifier)).matched();
            distributionResponse = responseOrException.response()
                    .orElseThrow(() -> new RuntimeException("Could not disable distribution"));
        }

        DeleteDistributionResponse deleteDistributionResponse = cloudFrontClient
                .deleteDistribution(builder -> builder
                        .id(distributionIdentifier)
                        .ifMatch(distributionResponse.eTag()));

        if (deleteDistributionResponse.sdkHttpResponse().isSuccessful()) {
            cloudFrontRepository.delete(distribution);
            String bucketName = response.distribution().distributionConfig().origins().items().stream().filter(origin -> origin.domainName().endsWith("s3.us-east-1.amazonaws.com")).findFirst().map(origin -> origin.domainName().replace(".s3.us-east-1.amazonaws.com", "")).orElse("");

            ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build());

            listObjectsResponse.contents().forEach(s3Object -> {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Object.key())
                        .build());
            });
        }
    }

    @Override
    public ResponseMessage disableOrEnable(String authorization, String distributionIdentifier, boolean command) {
        User user = utilsMethods.extractUsernameFromAuthorizationHeader(authorization);
        AWS.API.entity.Distribution distribution = cloudFrontRepository.findByDistributionIdentifier(distributionIdentifier);
        if(distribution == null) throw new EntityNotFoundException("There is no distribution with this name");

        if(!distribution.getUser().getId().equals(user.getId())) throw new EntityNotFoundException("This user can not access this distribution");

        GetDistributionRequest request = GetDistributionRequest.builder()
                .id(distribution.getDistributionIdentifier())
                .build();

        GetDistributionResponse response = cloudFrontClient.getDistribution(request);

        boolean enabled = response.distribution().distributionConfig().enabled();

        if(enabled == command) throw new EntityExistsException("Application already in this state");

        String tag = response.eTag();
        DistributionConfig distConfig = response.distribution().distributionConfig();

        cloudFrontClient.updateDistribution(builder -> builder
                .id(distributionIdentifier)
                .distributionConfig(builder1 -> builder1
                        .cacheBehaviors(distConfig.cacheBehaviors())
                        .defaultCacheBehavior(distConfig.defaultCacheBehavior())
                        .enabled(command)
                        .origins(distConfig.origins())
                        .comment(distConfig.comment())
                        .callerReference(distConfig.callerReference())
                        .defaultCacheBehavior(distConfig.defaultCacheBehavior())
                        .priceClass(distConfig.priceClass())
                        .aliases(distConfig.aliases())
                        .logging(distConfig.logging())
                        .defaultRootObject(distConfig.defaultRootObject())
                        .customErrorResponses(distConfig.customErrorResponses())
                        .httpVersion(distConfig.httpVersion())
                        .isIPV6Enabled(distConfig.isIPV6Enabled())
                        .restrictions(distConfig.restrictions())
                        .viewerCertificate(distConfig.viewerCertificate())
                        .webACLId(distConfig.webACLId())
                        .originGroups(distConfig.originGroups()))
                .ifMatch(tag));

        return new ResponseMessage("Application is updating its status");
    }

    @Override
    public void uploadDirectoryToS3Bucket(MultipartFile zipFile, String targetBucket, String authorization) throws IOException {
        User user = utilsMethods.extractUsernameFromAuthorizationHeader(authorization);
        S3Bucket foundBucket = s3BucketRepository.findByBucketIdentifier(targetBucket);
        if (foundBucket == null) {
            throw new EntityNotFoundException("There is no bucket with this name");
        }

        if (!foundBucket.getUser().getId().equals(user.getId())) {
            throw new EntityNotFoundException("User is not allowed to use this bucket");
        }

        ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                .bucket(targetBucket)
                .maxKeys(1)
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(listObjectsRequest);
        if(!response.contents().isEmpty()) throw new EntityExistsException("A bucket needs to be empty to perform the upload");

        String sourceBucket = "zip-file-angulars";
        String zipFileKey = zipFile.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(sourceBucket)
                .key(zipFileKey)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(zipFile.getInputStream(), zipFile.getSize()));


        StartBuildRequest request = StartBuildRequest.builder()
                .projectName("angular-deploy-code-build")
                .environmentVariablesOverride(
                        EnvironmentVariable.builder().name("ZIP_FILE_KEY").value(zipFileKey).build(),
                        EnvironmentVariable.builder().name("TARGET_BUCKET").value(targetBucket).build()
                )
                .build();

        StartBuildResponse buildResponse = codeBuildClient.startBuild(request);
        String buildId = buildResponse.build().id();

        // Wait for the build to finish
        Build build = waitForBuildCompletion(buildId);

        // If the build is successful, create the CloudFront distribution
        if (build.buildStatus().toString().equals("SUCCEEDED")) {
            String domainName = this.createDistribution(s3Client, targetBucket, user);
            sesService.sendEmail("arenasportcenter2000@gmail.com", user.getUsername(),"Application Deployment Successful", "Your content was loaded. You can access it at: " + domainName);
        } else {
            // Handle failed build scenario
            sesService.sendEmail("arenasportcenter2000@gmail.com", user.getUsername(),"Application Deployment Failure ", "Your application can not be deployed. Something went wrong. Please try again ");
            throw new EntityExistsException("An error was encountered when deploying the application");
        }
    }

    private Build waitForBuildCompletion(String buildId) {
        // Polling the build status until it completes
        while (true) {
            BatchGetBuildsRequest batchGetBuildsRequest = BatchGetBuildsRequest.builder()
                    .ids(buildId)
                    .build();

            BatchGetBuildsResponse batchGetBuildsResponse = codeBuildClient.batchGetBuilds(batchGetBuildsRequest);
            Build build = batchGetBuildsResponse.builds().get(0);

            if (build.buildStatus().toString().equals("SUCCEEDED") || build.buildStatus().toString().equals("FAILED")) {
                return build; // Build is completed
            }

            try {
                // Wait for a short period before checking the status again
                Thread.sleep(4000); // 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted while waiting for build to complete.", e);
            }
        }
    }

}