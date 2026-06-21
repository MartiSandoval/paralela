import java.util.ArrayList;

/**
 * Harness de debug standalone para verificar la comunicacion con la red
 * distribuida de NodoServidor, sin levantar la UI de JavaFX.
 *
 * No reimplementa nada de la logica de red: llama a los mismos metodos
 * publicos de ClienteRed que usa la app real (MainApp/CatalogoController),
 * para que el comportamiento que se prueba aca sea exactamente el mismo
 * que ve el usuario final. Esto tambien significa que el logging de
 * Lamport y la tolerancia a fallos por rotacion de nodos se ejercitan
 * igual que en producción.
 *
 * Ejecucion: java PruebaNodos
 * (o desde el IDE apuntando a esta clase como entry point puntual, sin
 * tocar el mainClass configurado en pom.xml para MainApp)
 */
public class PruebaNodos {

    public static void main(String[] args) {
        System.out.println("===== PruebaNodos: secuencia de diagnostico de la red distribuida =====\n");

        System.out.println("--- Paso 1: solicitar catalogo ---");
        ArrayList<Pelicula> catalogo = ClienteRed.solicitarCatalogo();

        if (catalogo == null || catalogo.isEmpty()) {
            System.err.println("\nFALLO: no se pudo obtener el catalogo. Todos los nodos estan caidos o no responden.");
            System.err.println("Verifica que al menos un NodoServidor este corriendo en los puertos configurados.");
            return;
        }

        System.out.println("\nCatalogo recibido: " + catalogo.size() + " peliculas.");
        for (Pelicula p : catalogo) {
            System.out.println("  - " + p.getTitulo());
        }

        Pelicula primera = catalogo.get(0);

        System.out.println("\n--- Paso 2: solicitar detalle de '" + primera.getTitulo() + "' ---");
        Pelicula detalle = ClienteRed.solicitarInfoPelicula(primera.getTitulo());

        if (detalle == null) {
            System.err.println("\nFALLO: no se pudo obtener el detalle de la pelicula.");
            return;
        }

        System.out.println("\nDetalle recibido:");
        System.out.println("  Titulo: " + detalle.getTitulo());
        System.out.println("  Anio: " + detalle.getAño());
        System.out.println("  Directores: " + String.join(", ", detalle.getDirector()));
        System.out.println("  Generos: " + String.join(", ", detalle.getGeneros()));

        System.out.println("\n--- Paso 3: iniciar streaming de '" + detalle.getTitulo() + "' ---");
        long inicio = System.currentTimeMillis();

        String rutaDescargada = ClienteRed.iniciarStreaming(
            detalle.getPath(),
            paquetes -> {
                if (paquetes % 100 == 0) {
                    System.out.println("  ... " + paquetes + " paquetes UDP recibidos");
                }
            }
        );

        long duracionMs = System.currentTimeMillis() - inicio;

        if (rutaDescargada == null) {
            System.err.println("\nFALLO: el streaming no se completo correctamente.");
            return;
        }

        System.out.println("\nStreaming completado en " + duracionMs + " ms.");
        System.out.println("Archivo descargado en: " + rutaDescargada);

        System.out.println("\n===== Diagnostico finalizado sin errores =====");
    }
}