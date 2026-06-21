import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.IntConsumer;

/**
 * Encapsula toda la comunicacion con ServidorCatalogo (TCP) y ServidorVideo (UDP).
 * Es la misma logica que antes vivia en Main.java, movida aqui para que pueda
 * ser invocada desde controladores de JavaFX sin depender de System.out/Scanner.
 */
public class ClienteRed {
    private static final String IP_SERVIDOR = "127.0.0.1";
    private static final int PUERTO_UDP_SERVER = 6000;
    private static final int[] PUERTOS_CATALOGO = {5000, 5001, 5002};

    public static int relojCliente = 0;

    /**
     * Pide el catalogo completo al primer nodo ServidorCatalogo que responda.
     * @return la lista de peliculas, o null si todos los nodos estan caidos.
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<Pelicula> solicitarCatalogo() {
        relojCliente++;

        for (int puertoDestino : PUERTOS_CATALOGO) {
            try (Socket s = new Socket(IP_SERVIDOR, puertoDestino);
                 ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {

                Mensaje msjEnvio = new Mensaje("SOLICITAR_CATALOGO", null, relojCliente, 0);
                out.writeObject(msjEnvio);
                out.flush();

                Mensaje msjRecibido = (Mensaje) in.readObject();

                relojCliente = Math.max(relojCliente, msjRecibido.getRelojLamport()) + 1;

                return (ArrayList<Pelicula>) msjRecibido.getPayload();

            } catch (Exception e) {
                // Nodo caido, se intenta con el siguiente puerto de la lista.
            }
        }
        return null;
    }

    /**
     * Pide el detalle de una pelicula por titulo al primer nodo que responda.
     * @return la Pelicula con sus datos, o null si todos los nodos estan caidos.
     */
    public static Pelicula solicitarInfoPelicula(String titulo) {
        relojCliente++;

        for (int puertoDestino : PUERTOS_CATALOGO) {
            try (Socket s = new Socket(IP_SERVIDOR, puertoDestino);
                 ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {

                Mensaje msjEnvio = new Mensaje("VER_DETALLE", titulo, relojCliente, 0);
                out.writeObject(msjEnvio);
                out.flush();

                Mensaje msjRecibido = (Mensaje) in.readObject();

                relojCliente = Math.max(relojCliente, msjRecibido.getRelojLamport()) + 1;

                return (Pelicula) msjRecibido.getPayload();

            } catch (Exception e) {
                // Nodo caido, se intenta con el siguiente puerto de la lista.
            }
        }
        return null;
    }

    /**
     * Pide el streaming UDP de una pelicula y lo vuelca en buffer_temporal.mp4.
     * Es una operacion bloqueante (igual que en Main.java original), por lo que
     * debe invocarse desde un hilo distinto al de JavaFX Application Thread.
     *
     * @param rutaVideo      identificador de la pelicula que espera ServidorVideo (mismo valor que ya se enviaba antes).
     * @param onPaquete      callback opcional invocado cada vez que llega un paquete UDP, con el total acumulado. Puede ser null.
     * @return la ruta absoluta del archivo descargado, o null si la transferencia fallo.
     */
    public static String iniciarStreaming(String rutaVideo, IntConsumer onPaquete) {
        File archivoBuffer = new File("buffer_temporal.mp4");

        try (DatagramSocket socketUDP = new DatagramSocket();
             FileOutputStream fos = new FileOutputStream(archivoBuffer)) {

            socketUDP.setSoTimeout(2000);

            String mensaje = "PLAY;" + rutaVideo;
            byte[] data = mensaje.getBytes();
            DatagramPacket peticion = new DatagramPacket(data, data.length,
                    InetAddress.getByName(IP_SERVIDOR), PUERTO_UDP_SERVER);
            socketUDP.send(peticion);

            byte[] buffer = new byte[640000];
            int paquetesRecibidos = 0;

            while (true) {
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                socketUDP.receive(paquete);

                fos.write(paquete.getData(), 0, paquete.getLength());
                fos.flush();

                paquetesRecibidos++;
                if (onPaquete != null) {
                    onPaquete.accept(paquetesRecibidos);
                }
            }

        } catch (java.net.SocketTimeoutException e) {
            // Fin de la transferencia: el servidor dejo de enviar paquetes.
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return archivoBuffer.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("Error critico en la red UDP: " + e.getMessage());
        }
        return null;
    }
}