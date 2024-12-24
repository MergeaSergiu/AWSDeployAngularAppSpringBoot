package AWS.API.service;

public interface SESService {

    void sendEmail(String from, String to, String subject, String bodyText);
}
