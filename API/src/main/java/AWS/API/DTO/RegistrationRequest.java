package AWS.API.DTO;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@Builder
public class RegistrationRequest {

    private String username;
    private String password;
    private String phoneNumber;

}
