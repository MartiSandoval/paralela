import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServidorVideo {
    public static int miPuerto = -1;
    private static final ExecutorService poolStreaming = Executors.newFixedThreadPool(5);
    
    // Lista de puertos designados para los servidores de Video
    private static final int[] PUERTOS_DISPONIBLES = {6000, 6001, 6002};

    public static void main(String[] args) {
        DatagramSocket socketPrincipal = null;

        // 1. ASIGNACIÓN AUTOMÁTICA DE PUERTO
        System.out.println("Buscando puerto disponible para iniciar Servidor de Video...");
        for (int puerto : PUERTOS_DISPONIBLES) {
            try {
                socketPrincipal = new DatagramSocket(puerto);
                miPuerto = puerto;
                break; // Logró tomar el puerto, sale del bucle
            } catch (SocketException e) {
                // Puerto ocupado, el bucle continúa intentando con el siguiente
            }
        }

        if (miPuerto == -1) {
            System.err.println("Error crítico: Todos los puertos de video (6000-6002) están ocupados.");
            return;
        }

        System.out.println("¡Éxito! Servidor de Video (UDP) corriendo automáticamente en el puerto: " + miPuerto);

        // 2. INTEGRACIÓN A LA TOPOLOGÍA
        // Iniciamos el escáner para presentarnos al resto de la red (Catálogos y otros Videos)
        new Thread(new GestorMembresia(miPuerto, "VIDEO")).start();

        // 3. BUCLE DE ESCUCHA DE STREAMING
        try {
            byte[] reciboBuffer = new byte[1024];

            while (true) {
                DatagramPacket peticion = new DatagramPacket(reciboBuffer, reciboBuffer.length);
                socketPrincipal.receive(peticion);

                String mensaje = new String(peticion.getData(), 0, peticion.getLength());
                System.out.println("Petición UDP recibida: " + mensaje);

                if (mensaje.startsWith("PLAY")) {
                    String rutaVideo = mensaje.split(";")[1];
                    Streaming tarea = new Streaming(
                        rutaVideo, 
                        peticion.getAddress(), 
                        peticion.getPort(), 
                        socketPrincipal
                    );
                    
                    poolStreaming.execute(tarea);
                }
            }
        } catch (Exception e) {
            System.err.println("Error crítico en Servidor de Video: " + e.getMessage());
        } finally {
            if (socketPrincipal != null && !socketPrincipal.isClosed()) {
                socketPrincipal.close();
            }
        }
    }
}