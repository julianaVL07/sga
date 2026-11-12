package co.edu.uniquindio.sga.domain.port;

/**
 * Puerto de las credenciales de los canales externos (10.3.1). El dominio decide cuándo se emite o se verifica
 * una credencial; cómo se genera al azar y cómo se cifra (BCrypt) lo resuelve la infraestructura.
 */
public interface CredencialesCanal {

    /** Credencial nueva, aleatoria y difícil de adivinar, en claro. */
    String generar();

    /** Hash de la credencial: es lo único que se guarda. */
    String cifrar(String credencial);

    /** ¿La credencial en claro corresponde a ese hash? */
    boolean coincide(String credencial, String hash);
}
