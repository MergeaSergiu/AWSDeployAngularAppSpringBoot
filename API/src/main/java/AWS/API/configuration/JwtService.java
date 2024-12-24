package AWS.API.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${secret.key.jwt}")
    private String secret;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;
    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    public String generateToken(String username, String role){
        return generateToken(username, role, jwtExpiration);
    }

    public String generateRefreshToken(String username, String role){
        return generateToken(username, role, refreshExpiration);
    }

    public String generateToken(String userName, String role, long expiration){
        Map<String,Object> extraClaims = new HashMap<>();
        extraClaims.put("role", role);
        return Jwts.builder()
                .setHeaderParam("alg", "HS512")
                .setHeaderParam("typ", "JWT")
                .setClaims(extraClaims)
                .setSubject(userName)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith( SignatureAlgorithm.HS512, getSignInKey())
                .compact();
    }

    public String extractUsername(String jwt){
        Claims claims = Jwts.parser()
                .setSigningKey(getSignInKey())
                .parseClaimsJws(jwt)
                .getBody();

        return claims.getSubject();
    }

    public String extractRole(String jwt){
        Claims claims = Jwts.parser()
                .setSigningKey(getSignInKey())
                .parseClaimsJws(jwt)
                .getBody();

        return claims.get("role").toString();
    }

    public boolean validateToken(String jwt){
        Date expirationDateToken = Jwts.parser()
                .setSigningKey(getSignInKey())
                .parseClaimsJws(jwt)
                .getBody()
                .getExpiration();

        return !expirationDateToken.before(new Date());
    }

    private Key getSignInKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.getJcaName());
    }
}
