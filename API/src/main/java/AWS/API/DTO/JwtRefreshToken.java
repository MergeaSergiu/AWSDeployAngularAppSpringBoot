package AWS.API.DTO;


import lombok.*;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JwtRefreshToken {

    private String refresh_JWT;
}
