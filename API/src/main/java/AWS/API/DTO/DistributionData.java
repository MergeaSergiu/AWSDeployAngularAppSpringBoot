package AWS.API.DTO;

public record DistributionData(String identifier,
                               String domainName,
                               String bucket,
                               String status,
                               boolean enabled) {}
