import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServidorVideo {
    private static final int PUERTO_ESCUCHA = 6000;
    private static final ExecutorService poolStreaming = Executors.newFixedThreadPool(5);

    public static void main(String[] args) {
        try (DatagramSocket socketPrincipal = new DatagramSocket(PUERTO_ESCUCHA)) {
            System.out.println("Servidor de Video (UDP) iniciado en el puerto " + PUERTO_ESCUCHA);

            byte[] reciboBuffer = new byte[1024];

            while (true) {
                DatagramPacket peticion = new DatagramPacket(reciboBuffer, reciboBuffer.length);
                socketPrincipal.receive(peticion);

                String mensaje = new String(peticion.getData(), 0, peticion.getLength());
                System.out.println("Petición recibida: " + mensaje);

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
        }
    }
}