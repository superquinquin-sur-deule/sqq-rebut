package org.superquinquin.releve;

/** Modification partielle d'une ligne : quantité et/ou motif (le motif ne vaut que pour une perte). */
public record UpdateLineRequest(Double qty, Long motifId) {
}
