package org.superquinquin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marque un champ de test à injecter avec le WireMockServer démarré par {@link WireMockOdooResource}. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectWireMock {
}
