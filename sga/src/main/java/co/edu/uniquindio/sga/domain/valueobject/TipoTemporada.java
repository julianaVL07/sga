package co.edu.uniquindio.sga.domain.valueobject;

/** Tipos de temporada del Anexo A: base obligatoria (F-05), media y alta. */
public enum TipoTemporada {
    BASE,
    MEDIA,
    ALTA;

    /** RP-01: la estancia mínima de fin de semana aplica en temporada media o alta. */
    public boolean exigeEstanciaMinimaFinDeSemana() {
        return this == MEDIA || this == ALTA;
    }
}
