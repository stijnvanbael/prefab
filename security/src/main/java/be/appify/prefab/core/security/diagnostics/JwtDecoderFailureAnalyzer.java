package be.appify.prefab.core.security.diagnostics;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Explains why Prefab's security filter chain cannot start without a {@link JwtDecoder}.
 *
 * <p>The default chain validates bearer tokens as JWTs. Spring Boot only creates the decoder when it knows where the
 * signing keys are.</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class JwtDecoderFailureAnalyzer extends AbstractFailureAnalyzer<NoSuchBeanDefinitionException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, NoSuchBeanDefinitionException cause) {
        if (cause.getNumberOfBeansFound() > 1 || !JwtDecoder.class.equals(cause.getBeanType())) {
            return null;
        }
        return new FailureAnalysis(
                "prefab-security validates bearer tokens as JWTs, but the application context has no JwtDecoder "
                        + "to verify their signature.",
                "Set spring.security.oauth2.resourceserver.jwt.issuer-uri (the issuer publishes its keys), or "
                        + "spring.security.oauth2.resourceserver.jwt.jwk-set-uri, or define a JwtDecoder bean. "
                        + "To replace the default security chain, define a bean named prefabSecurityFilterChain.",
                rootFailure);
    }
}
