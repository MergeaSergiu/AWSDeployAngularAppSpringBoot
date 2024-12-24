package AWS.API.DTO;

public record LoginResponseDTO(String access_JWT,
                               String refresh_JWT,
                               String role){}
