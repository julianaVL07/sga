package co.edu.uniquindio.sga.infrastructure.ia;

import co.edu.uniquindio.sga.domain.port.SugeridorGravedad;
import co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad;
import co.edu.uniquindio.sga.domain.valueobject.SugerenciaGravedad;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adaptador LOCAL del puerto SugeridorGravedad (10.2): reglas por palabras clave, sin red y sin clave.
 * Es el que corre por defecto y en las pruebas (sga.ia.proveedor=local).
 * Se revisan primero las palabras de riesgo (ALTA), luego las de daño o mal funcionamiento (MEDIA); si no aparece
 * ninguna, la sugerencia es BAJA (faltantes, aseo, detalles menores). La descripción se compara sin tildes y en
 * minúsculas, así "Eléctrico" y "electrico" cuentan igual.
 */
public class SugeridorGravedadLocal implements SugeridorGravedad {

    /** Palabras (o raíces) que indican riesgo para las personas o para el apartamento. */
    private static final List<String> PALABRAS_ALTA = List.of(
            "gas", "fuga\\w*", "electric\\w*", "cortocircuito\\w*", "chispa\\w*", "corrientazo\\w*",
            "cable\\w* pelado\\w*", "inunda\\w*", "incendio\\w*", "fuego", "humo",
            "explosi\\w*", "herid\\w*", "sangr\\w*", "desplom\\w*", "derrumb\\w*", "techo caido");

    /** Palabras que indican un daño o algo que no funciona, sin riesgo inmediato. */
    private static final List<String> PALABRAS_MEDIA = List.of(
            "rot[oa]s?", "dañad[oa]s?", "dano\\w*", "averia\\w*", "no funciona\\w*", "no sirve\\w*",
            "no enciende\\w*", "no prende\\w*", "no cierra\\w*", "gote\\w*", "filtraci\\w*", "humedad",
            "atascad[oa]s?", "tapad[oa]s?", "obstruid[oa]s?", "quebrad[oa]s?", "rajad[oa]s?", "cerradura\\w*",
            "aire acondicionado", "nevera", "sanitario\\w*", "inodoro\\w*", "ducha\\w*", "calentador\\w*");

    private static final Pattern PATRON_ALTA = compilar(PALABRAS_ALTA);
    private static final Pattern PATRON_MEDIA = compilar(PALABRAS_MEDIA);

    @Override
    public Optional<SugerenciaGravedad> sugerir(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return Optional.empty();
        }
        String texto = normalizar(descripcion);

        Matcher alta = PATRON_ALTA.matcher(texto);
        if (alta.find()) {
            return Optional.of(new SugerenciaGravedad(GravedadNovedad.ALTA,
                    "Menciona \"" + alta.group() + "\", un posible riesgo para las personas o el apartamento"));
        }
        Matcher media = PATRON_MEDIA.matcher(texto);
        if (media.find()) {
            return Optional.of(new SugerenciaGravedad(GravedadNovedad.MEDIA,
                    "Menciona \"" + media.group() + "\", un daño o algo que no funciona"));
        }
        return Optional.of(new SugerenciaGravedad(GravedadNovedad.BAJA,
                "No menciona riesgos ni daños de funcionamiento"));
    }

    /** Minúsculas y sin tildes: "Fuga de GAS en la cocina" → "fuga de gas en la cocina". */
    static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    /** Une las palabras en un solo patrón con límites de palabra, para que "gas" no coincida con "gaseosa". */
    private static Pattern compilar(List<String> palabras) {
        List<String> normalizadas = palabras.stream().map(SugeridorGravedadLocal::normalizar).toList();
        return Pattern.compile("\\b(?:" + String.join("|", normalizadas) + ")\\b");
    }
}
