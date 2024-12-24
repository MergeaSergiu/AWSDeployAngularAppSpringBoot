package AWS.API.controller;

import AWS.API.DTO.ResponseMessage;
import AWS.API.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedList;

@Tag(name = "Operation for S3 Buckets")
@AllArgsConstructor
@RestController
@RequestMapping("/api/aws/s3")
public class AWSS3Controller {

    private final S3Service s3Service;

    @Operation(summary = "Get a list of all bucket created by the current user")
    @GetMapping("/buckets")
    public ResponseEntity<LinkedList<String>> getBucketList(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization){
        return ResponseEntity.ok(s3Service.getBucketList(authorization));
    }

    @Operation(summary = "Create a bucket that will store the application")
    @PostMapping(path = "/buckets")
    public ResponseEntity<ResponseMessage> createBucket(@RequestParam String bucketName, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization){
        return ResponseEntity.status(HttpStatus.CREATED).body(s3Service.createBucket(bucketName, authorization));
    }

    @Operation(summary = "Delete a bucket by its Identifier")
    @DeleteMapping("/buckets")
    public ResponseEntity<ResponseMessage> deleteBucket(@RequestParam String bucketName, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization){
        return ResponseEntity.status(HttpStatus.CREATED).body(s3Service.deleteBucket(bucketName, authorization));
    }

}
