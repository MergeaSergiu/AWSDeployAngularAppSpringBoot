package AWS.API.service;


import AWS.API.DTO.ResponseMessage;

import java.util.LinkedList;

public interface S3Service {

    LinkedList<String> getBucketList(String authorization);

    ResponseMessage createBucket(String bucket, String authorization);


    ResponseMessage deleteBucket(String bucketName, String authorization);




}
