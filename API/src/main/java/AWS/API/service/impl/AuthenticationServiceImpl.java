package AWS.API.service.impl;

import AWS.API.DTO.*;
import AWS.API.entity.User;
import AWS.API.configuration.JwtService;
import AWS.API.entity.Role;
import AWS.API.exception.RefreshTokenExpired;
import AWS.API.repository.RoleRepository;
import AWS.API.repository.UserRepository;
import AWS.API.service.AuthenticationService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@AllArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public RegistrationResponse register(RegistrationRequest registrationRequest) {
        User foundUser = userRepository.findByUsername(registrationRequest.getUsername()).orElse(null);
        if(foundUser != null) throw new EntityExistsException("There is already a user registered with this email");
        Role role = roleRepository.findByName("USER");
        if(role == null) throw new EntityNotFoundException("Role does not exist");
        if(registrationRequest.getPassword().length() < 8){
            throw new EntityNotFoundException("Password must be at least 8 characters");
        }

        User user = User.builder()
                .username(registrationRequest.getUsername())
                .password(passwordEncoder.encode(registrationRequest.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);

        return new RegistrationResponse("Account was created successfully. Please logIn");
    }

    @Override
    public LoginResponseDTO authenticate(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername()).orElseThrow(() -> new EntityNotFoundException("User does not exist"));
         authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        return new LoginResponseDTO(
                jwtService.generateToken(user.getUsername(), user.getRole().getName()),
                jwtService.generateRefreshToken(user.getUsername(), user.getRole().getName()),
                user.getRole().getName()
        );
    }

    @Override
    public LoginResponseDTO generateToken(JwtRefreshToken refreshToken) {
        if(!jwtService.validateToken(refreshToken.getRefresh_JWT())) throw new RefreshTokenExpired("Refresh Token expired");
        String username = jwtService.extractUsername(refreshToken.getRefresh_JWT());
        if(username == null) throw new EntityNotFoundException("Refresh token does not exist");
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User does not exist"));
        String access_token = jwtService.generateToken(user.getUsername(), user.getRole().getName());
        String role = jwtService.extractRole(refreshToken.getRefresh_JWT());
        return new LoginResponseDTO(
                access_token,
                refreshToken.getRefresh_JWT(),
                role);
    }
}
