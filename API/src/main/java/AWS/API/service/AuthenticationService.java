package AWS.API.service;

import AWS.API.DTO.*;

public interface AuthenticationService {

    RegistrationResponse register(RegistrationRequest registrationRequest);

    LoginResponseDTO authenticate(LoginRequest loginRequest);

    LoginResponseDTO generateToken(JwtRefreshToken refreshToken);
}
