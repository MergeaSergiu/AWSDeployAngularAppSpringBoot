package AWS.API.controller;

import AWS.API.DTO.*;
import AWS.API.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Tag(name = "User authentication")
@AllArgsConstructor
@RequestMapping("/api/aws/auth")
@RestController
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Operation(summary = "Sign up an user")
    @SecurityRequirement(name = "The resource can be access by all users")
    @PostMapping("register")
    public ResponseEntity<RegistrationResponse> register(@RequestBody RegistrationRequest registrationRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.register(registrationRequest));
    }

    @Operation(summary = "Login an user")
    @SecurityRequirement(name = "The resource can be access by all users")
    @PostMapping("login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequest loginRequest){
        LoginResponseDTO userData  = authenticationService.authenticate(loginRequest);
        return ResponseEntity.status(HttpStatus.OK).body(userData);
    }

    @Operation(summary = "Create a new access_token when the current one expires")
    @SecurityRequirement(name = "A valid token is necessary to access this endpoint")
    @PostMapping("refresh")
    public ResponseEntity<LoginResponseDTO> refresh(@RequestBody JwtRefreshToken refreshToken){
        LoginResponseDTO refreshData = authenticationService.generateToken(refreshToken);
        return ResponseEntity.status(HttpStatus.OK).body(refreshData);
    }

}
