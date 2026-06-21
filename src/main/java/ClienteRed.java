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

public class ClienteRed {

    private static final String ID_CLIENTE = "Cliente JavaFX";

    private static final String[][] NODOS_CONOCIDOS = {
        {"127.0.0.1", "5001", "6001"},
        {"127.0.0.1", "5002", "6002"},
        {"127.0.0.1", "5003", "6003"}
    };

    private static int nodoActual = 0;
    private static int relojLamport = 0;

    private static synchronized void eventoLocal(String evento) {
        relojLamport++;
        System.out.println("[LAMPORT T=" + relojLamport + " | " + ID_CLIENTE + "] " + evento);
    }

    private static synchronized void sincronizarReloj(int relojExterno, String evento) {
        relojLamport = Math.max(relojLamport, relojExterno) + 1;
        System.out.println("[LAMPORT T=" + relojLamport + " | " + ID_CLIENTE + "] Sincronizacion: " + evento);
    }

    @SuppressWarnings("unchecked")
    public static synchronized ArrayList<Pelicula> solicitarCatalogo() {
        int intentos = 0;

        while (intentos < NODOS_CONOCIDOS.length) {
            String ip = NODOS_CONOCIDOS[nodoActual][0];
            int puertoTCP = Integer.parseInt(NODOS_CONOCIDOS[nodoActual][1]);

            try (Socket s = new Socket(ip, puertoTCP);
                 ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {

                eventoLocal("Enviando peticion SOLICITAR_CATALOGO al Nodo " + (nodoActual + 1));
                Mensaje msjEnvio = new Mensaje("SOLICITAR_CATALOGO", null, relojLamport, ID_CLIENTE);
                out.writeObject(msjEnvio);
                out.flush();

                Mensaje msjRecibido = (Mensaje) in.readObject();
                sincronizarReloj(msjRecibido.getRelojLamport(), "Catalogo descargado con exito");

                return (ArrayList<Pelicula>) msjRecibido.getPayload();

            } catch (Exception e) {
                System.out.println("[Tolerancia a fallos] Nodo " + (nodoActual + 1) + " no responde. Saltando al siguiente nodo...");
                nodoActual = (nodoActual + 1) % NODOS_CONOCIDOS.length;
                intentos++;
            }
        }
        return null;
    }

    public static synchronized Pelicula solicitarInfoPelicula(String titulo) {
        int intentos = 0;

        while (intentos < NODOS_CONOCIDOS.length) {
            String ip = NODOS_CONOCIDOS[nodoActual][0];
            int puertoTCP = Integer.parseInt(NODOS_CONOCIDOS[nodoActual][1]);

            try (Socket s = new Socket(ip, puertoTCP);
                 ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {

                eventoLocal("Solicitando detalles de la pelicula: " + titulo);
                Mensaje msjEnvio = new Mensaje("VER_DETALLE", titulo, relojLamport, ID_CLIENTE);
                out.writeObject(msjEnvio);
                out.flush();

                Mensaje msjRecibido = (Mensaje) in.readObject();
                sincronizarReloj(msjRecibido.getRelojLamport(), "Detalles recibidos de Nodo " + (nodoActual + 1));

                return (Pelicula) msjRecibido.getPayload();

            } catch (Exception e) {
                System.out.println("[Tolerancia a fallos] Nodo " + (nodoActual + 1) + " caido al pedir detalles. Rotando...");
                nodoActual = (nodoActual + 1) % NODOS_CONOCIDOS.length;
                intentos++;
            }
        }
        return null;
    }

    /**
     * Pide el streaming UDP de una pelicula al nodo actual y lo vuelca en
     * buffer_temporal.mp4. Es una operacion bloqueante, por lo que debe
     * invocarse desde un hilo distinto al de JavaFX Application Thread.
     *
     * Si el nodo actual no responde al PLAY inicial (timeout sin ningun
     * paquete recibido), rota al siguiente nodo igual que hacen
     * solicitarCatalogo y solicitarInfoPelicula. El SocketTimeoutException
     * tras haber recibido datos sigue interpretandose como fin normal de
     * la transferencia.
     *
     * @param rutaVideo identificador de la pelicula que espera NodoServidor.
     * @param onPaquete callback opcional invocado por cada paquete UDP recibido, con el total acumulado. Puede ser null.
     * @return la ruta absoluta del archivo descargado, o null si todos los nodos fallaron.
     */
    public static String iniciarStreaming(String rutaVideo, IntConsumer onPaquete) {
        File archivoBuffer = new File("buffer_temporal.mp4");
        int intentos = 0;

        while (intentos < NODOS_CONOCIDOS.length) {
            String ip = NODOS_CONOCIDOS[nodoActual][0];
            int puertoUDP = Integer.parseInt(NODOS_CONOCIDOS[nodoActual][2]);

            try (DatagramSocket socketUDP = new DatagramSocket();
                 FileOutputStream fos = new FileOutputStream(archivoBuffer)) {

                socketUDP.setSoTimeout(2000);

                eventoLocal("Iniciando streaming UDP desde el Nodo " + (nodoActual + 1));
                byte[] data = ("PLAY;" + rutaVideo).getBytes();
                DatagramPacket peticion = new DatagramPacket(data, data.length,
                        InetAddress.getByName(ip), puertoUDP);
                socketUDP.send(peticion);

                byte[] buffer = new byte[64000];
                int paquetesRecibidos = 0;

                while (true) {
                    DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                    try {
                        socketUDP.receive(paquete);
                    } catch (java.net.SocketTimeoutException ste) {
                        if (paquetesRecibidos == 0) {
                            // Timeout sin datos: el nodo no respondio al PLAY → rotar
                            throw ste;
                        }
                        // Timeout tras recibir datos: fin de transferencia normal
                        break;
                    }
                    fos.write(paquete.getData(), 0, paquete.getLength());
                    fos.flush();
                    paquetesRecibidos++;
                    if (onPaquete != null) onPaquete.accept(paquetesRecibidos);
                }

                try { Thread.sleep(500); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                eventoLocal("Transferencia completada desde el Nodo " + (nodoActual + 1));
                return archivoBuffer.getAbsolutePath();

            } catch (java.net.SocketTimeoutException e) {
                System.out.println("[Tolerancia a fallos] Nodo " + (nodoActual + 1) + " no responde al streaming. Rotando...");
                nodoActual = (nodoActual + 1) % NODOS_CONOCIDOS.length;
                intentos++;
            } catch (Exception e) {
                System.err.println("[Tolerancia a fallos] Error en Nodo " + (nodoActual + 1) + ": " + e.getMessage() + ". Rotando...");
                nodoActual = (nodoActual + 1) % NODOS_CONOCIDOS.length;
                intentos++;
            }
        }

        System.err.println("[Tolerancia a fallos] Todos los nodos fallaron en streaming.");
        return null;
    }
}