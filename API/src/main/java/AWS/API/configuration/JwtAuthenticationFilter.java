package AWS.API.configuration;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@AllArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private final JwtService jwtService;

    @Autowired
    private final UserDetailsService clientDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String headerAuthorization = request.getHeader("Authorization");
            if (headerAuthorization == null || !headerAuthorization.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }
            String JWT = headerAuthorization.substring(7);
            String username = jwtService.extractUsername(JWT);

            if (SecurityContextHolder.getContext().getAuthentication() == null && username != null) {
                UserDetails userDetails = this.clientDetailsService.loadUserByUsername(username);
                if (jwtService.validateToken(JWT)) {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException ex) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().println("{ \"error\": \"Session has expired\" }");
        }
    }
}
