package AWS.API.service.impl;

import AWS.API.service.SESService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
public class SESServiceImpl implements SESService {

    private final Region region;
    private final SesClient sesClient;

    public SESServiceImpl(@Value("${aws.accessKeyId}") String accessKeyId,
                      @Value("${aws.secretAccessKey}") String secretAccessKey,
                      @Value("${aws.region}") String region){

        this.region = Region.of(region);
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        this.sesClient = SesClient.builder()
                .region(this.region)
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();

    }

     public void sendEmail(String from, String to, String subject, String bodyText) {
        Destination destination = Destination.builder()
                .toAddresses(to)
                .build();

        Content subjectContent = Content.builder()
                .data(subject)
                .build();

        Content bodyContent = Content.builder()
                .data(bodyText)
                .build();

        Body body = Body.builder()
                .text(bodyContent)
                .build();

        Message message = Message.builder()
                .subject(subjectContent)
                .body(body)
                .build();

        SendEmailRequest emailRequest = SendEmailRequest.builder()
                .destination(destination)
                .message(message)
                .source(from)
                .build();

        try {
            sesClient.sendEmail(emailRequest);
        } catch (SesException e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
}
