package unmsm.edu.pe.tesis.domain.services;

import unmsm.edu.pe.shared.response.PageResponse;
import unmsm.edu.pe.tesis.application.dto.ExpedienteBandejaItem;

import java.util.UUID;

/** Secretaría · Etapa 5 (Defensa) — paso 1: recibir el expediente y comunicar al Coordinador. */
public interface SecretariaDefensaService {

    PageResponse<ExpedienteBandejaItem> bandeja(String buscar, int page, int size);

    /** La Secretaría recibe el expediente y lo comunica al Coordinador del Programa. */
    void recibir(UUID tesisId);
}
