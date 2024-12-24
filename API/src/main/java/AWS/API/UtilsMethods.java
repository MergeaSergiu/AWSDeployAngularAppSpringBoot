package AWS.API;


import AWS.API.configuration.JwtService;
import AWS.API.entity.User;
import AWS.API.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UtilsMethods {

    @Autowired
    private final JwtService jwtService;

    @Autowired
    private final UserRepository userRepository;

    public User extractUsernameFromAuthorizationHeader(String authorization){
        String jwt = authorization.substring(7);
        String username = jwtService.extractUsername(jwt);
        return userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User does not exist"));
    }



}
