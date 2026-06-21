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

/*
Encapsula toda la comunicacion con la red distribuida de NodoServidor
(TCP para catalogo/detalle, UDP para streaming de video).

Es el unico cliente real de la red: tanto MainApp (la app JavaFX) como
cualquier clase de prueba/debug (ej. PruebaNodos) deben invocar estos
metodos en vez de reimplementar la logica de sockets, para que la
tolerancia a fallos y el reloj de Lamport se mantengan consistentes sin
importar quien los use.

El logging de cada evento (envio, sincronizacion, fallos de nodo) se hace
por System.out/System.err. Esto es deliberado: no ensucia la UI de
JavaFX (que nunca lee la salida estandar), pero sí queda visible en la
terminal donde se ejecute el proceso, sea "mvn javafx:run" o una clase
de prueba standalone.
*/
public class ClienteRed {

    private static final String ID_CLIENTE = "Cliente JavaFX";

    // Tabla de nodos conocidos: {ip, puertoTCP, puertoUDP}. 3 Nodos
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

    public static String iniciarStreaming(String rutaVideo, IntConsumer onPaquete) {
        File archivoBuffer = new File("buffer_temporal.mp4");
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
            eventoLocal("Transferencia completada desde el Nodo " + (nodoActual + 1));
            return archivoBuffer.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("Error critico en la red UDP: " + e.getMessage());
        }
        return null;
    }
}