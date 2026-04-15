package br.com.ada.classes.meetingroom.service;

import br.com.ada.classes.meetingroom.model.LoggedUser;
import br.com.ada.classes.meetingroom.model.User;
import br.com.ada.classes.meetingroom.resource.auth.TokenResponse;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.Duration;

@ApplicationScoped
public class AuthService implements CurrentUserService {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @Inject
    JsonWebToken jwt;

    private Argon2 argon2;

    @PostConstruct
    void init() {
        argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
    }

    @Override
    public LoggedUser getLoggedUser() {
        if (jwt.getName() == null) {
            throw new NotAuthorizedException("Nenhum usuario autenticado na requisicao atual");
        }

        return new LoggedUser(
                getUserId(),
                jwt.getName(),
                jwt.getClaim("name"),
                getRole()
        );
    }

    private Long getUserId(){
        return Long.parseLong(jwt.getClaim("userId").toString());
    }

    private String getRole(){
        return jwt.getGroups()
                .stream()
                .findFirst()
                .orElse("USER");
    }

    public TokenResponse login(String username, String password) {
        User user = User.find("username", username).firstResult();
        validatePassword(user, password);
        String token = generateToken(user);
        return new TokenResponse(
                token,
                user.getUsername(),
                user.getName(),
                user.getRole().name()
        );
    }

    private void validatePassword(User user, String password) {
        boolean approve = user != null
                && argon2.verify(user.getPassword(), password.toCharArray());

        if (!approve) {
            throw new NotAuthorizedException("Credenciais invalidas");
        }
    }

    private String generateToken(User user) {
        return Jwt.issuer(issuer)
                .upn(user.getUsername())
                .groups(user.getRole().name())
                .claim("userId", user.id)
                .claim("name", user.getName())
                .expiresIn(Duration.ofMinutes(30))
                .sign();
    }
}
