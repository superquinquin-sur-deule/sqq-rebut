package org.superquinquin.brevo;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

@ConfigMapping(prefix = "brevo")
public interface BrevoConfig {

    /** Surchargée par WireMock dans les tests. */
    @WithDefault("https://api.brevo.com")
    String baseUrl();

    /** Clé API transactionnelle : secret d'environnement, jamais exposé à l'interface ni aux logs. */
    Optional<String> apiKey();

    Sender sender();

    interface Sender {
        String email();

        @WithDefault("Rebut SuperQuinquin")
        String name();
    }
}
