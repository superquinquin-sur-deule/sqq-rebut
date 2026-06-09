package org.superquinquin.odoo;

import io.quarkus.logging.Log;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
public class OdooExceptionMapper implements ExceptionMapper<OdooException> {

    @Override
    public Response toResponse(OdooException e) {
        Log.error("Erreur Odoo", e);
        return Response.status(502)
                .entity(Map.of("error", "odoo", "message", String.valueOf(e.getMessage())))
                .build();
    }
}
