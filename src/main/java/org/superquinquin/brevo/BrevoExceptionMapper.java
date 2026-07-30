package org.superquinquin.brevo;

import io.quarkus.logging.Log;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
public class BrevoExceptionMapper implements ExceptionMapper<BrevoException> {

    @Override
    public Response toResponse(BrevoException e) {
        Log.error("Erreur Brevo", e);
        return Response.status(502)
                .entity(Map.of("error", "brevo", "message", String.valueOf(e.getMessage())))
                .build();
    }
}
