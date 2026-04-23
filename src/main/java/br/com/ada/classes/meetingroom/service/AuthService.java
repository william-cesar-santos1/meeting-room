package br.com.ada.classes.meetingroom.service;

import br.com.ada.classes.meetingroom.exception.AuthenticationException;
import br.com.ada.classes.meetingroom.model.LoggedUser;
import br.com.ada.classes.meetingroom.model.User;
import br.com.ada.classes.meetingroom.resource.auth.TokenResponse;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.time.Duration;

@ApplicationScoped
public class AuthService implements CurrentUserService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);

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
            LOG.warn("Tentativa de acesso sem usuário autenticado no contexto JWT");
            throw new AuthenticationException("Nenhum usuario autenticado na requisicao atual");
        }
        LOG.debugf("Usuário autenticado obtido do token: username='%s'", jwt.getName());
        return new LoggedUser(
                getUserId(),
                jwt.getName(),
                jwt.getClaim("name"),
                getRole()
        );
    }

    private Long getUserId() {
        return Long.parseLong(jwt.getClaim("userId").toString());
    }

    private String getRole() {
        return jwt.getGroups()
                .stream()
                .findFirst()
                .orElse("USER");
    }

    @WithSpan("AuthService.login")
    public TokenResponse login(@SpanAttribute("auth.username") String username, String password) {
        LOG.infof("Tentativa de login para username='%s'", username);
        User user = User.find("username", username).firstResult();
        validatePassword(user, password);
        String token = generateToken(user);
        Span.current().setAttribute("auth.user.id", user.id);
        Span.current().setAttribute("auth.role", user.getRole().name());
        LOG.infof("Login bem-sucedido para username='%s'", username);
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
            LOG.warnf("Falha de autenticação: credenciais inválidas");
            Span.current().setStatus(StatusCode.ERROR, "credenciais invalidas");
            Span.current().setAttribute("auth.failed", true);
            throw new AuthenticationException("Credenciais invalidas");
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
