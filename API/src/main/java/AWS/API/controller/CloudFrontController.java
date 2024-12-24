package AWS.API.controller;

import AWS.API.DTO.DistributionData;
import AWS.API.DTO.ResponseMessage;
import AWS.API.service.CloudFrontService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@Tag(name ="Operation for deploying the application")
@AllArgsConstructor
@RestController
@RequestMapping("/api/aws/cloudFront")
public class CloudFrontController {
    private final CloudFrontService cloudFrontService;

    @Operation(summary = "Get a list of all applications for the current user")
    @GetMapping("/distributions")
    public ResponseEntity<List<DistributionData>> getDistributions(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization){
        return ResponseEntity.ok(cloudFrontService.getDistributions(authorization));
    }

    @Operation(summary = "Delete a distribution that allow access to the application")
    @DeleteMapping("/distributions")
    public ResponseEntity<Void> deleteDistribution(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization, @RequestParam String distributionIdentifier) throws InterruptedException {
        cloudFrontService.deleteDistribution(authorization,distributionIdentifier);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Update the status of a distribution: activated/disabled")
    @PostMapping("/distributions")
    public ResponseEntity<ResponseMessage> updateDistributionState(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization, @RequestParam String distributionIdentifier, @RequestParam boolean command){
        return ResponseEntity.status(HttpStatus.OK).body(cloudFrontService.disableOrEnable(authorization, distributionIdentifier, command));
    }

    @Operation(summary = "Upload a .zip archive with the Angular project")
    @PostMapping(path = "/distribution/uploads", consumes = {MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Void> uploadDirectory(@RequestParam MultipartFile multipartFile, @RequestParam String bucketName, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) throws IOException, InterruptedException {
        cloudFrontService.uploadDirectoryToS3Bucket(multipartFile,bucketName, authorization);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
