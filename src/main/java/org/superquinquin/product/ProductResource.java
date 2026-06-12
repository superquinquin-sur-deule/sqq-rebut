package org.superquinquin.product;

import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Produits")
public class ProductResource {

    @Inject
    ProductService service;

    @GET
    @Operation(operationId = "searchProducts",
            summary = "Chercher des produits par nom (ilike, max 10, produits avec code-barres uniquement)")
    public List<Product> search(@QueryParam("q") String q) {
        Log.info("Recherche de produits par nom : " + q);
        return service.searchByName(q);
    }

    @GET
    @Path("/{barcode}")
    @Operation(operationId = "getProductByBarcode", summary = "Chercher un produit par code-barres (EAN)")
    public Product byBarcode(@PathParam("barcode") String barcode) {
        Log.info("Recherche du produit par code-barres : " + barcode);
        return service.findByBarcode(barcode)
                .orElseThrow(() -> new NotFoundException("Produit inconnu pour le code-barres " + barcode));
    }
}
