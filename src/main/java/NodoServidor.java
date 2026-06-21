import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NodoServidor {
    private final int idNodo;
    private final int puertoTCP;
    private final int puertoUDP;
    private Catalogo baseDeDatos;

    // Utilizamos los mismos pools de hilos que ya tenías en tus servidores por separado
    private final ExecutorService poolTCP = Executors.newFixedThreadPool(10);
    private final ExecutorService poolUDP = Executors.newFixedThreadPool(5);

    public NodoServidor(int idNodo, int puertoTCP, int puertoUDP) {
        this.idNodo = idNodo;
        this.puertoTCP = puertoTCP;
        this.puertoUDP = puertoUDP;
        cargarCatalogo();
    }

    private void cargarCatalogo() {
        ArrayList<String> datosPeliculas = new ArrayList<>();
        // Adaptado de tu ServidorCatalogo.java original
        try (InputStream is = NodoServidor.class.getResourceAsStream("/peliculas/lista_peliculas.txt");
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            
            if (is == null) throw new FileNotFoundException("No se encontró el archivo lista_peliculas.txt");

            String linea;
            while ((linea = br.readLine()) != null) {
                if (!linea.trim().isEmpty()) {
                    datosPeliculas.add(linea);
                }
            }
            this.baseDeDatos = new Catalogo(datosPeliculas, datosPeliculas.size());
            System.out.println("[Nodo " + idNodo + "] Base de datos cargada: " + datosPeliculas.size() + " películas.");

        } catch (Exception e) {
            System.err.println("[Nodo " + idNodo + "] FALLO CRÍTICO: No se pudo cargar el catálogo.");
            e.printStackTrace();
        }
    }

    public void iniciar() {
        // Levantamos los servicios TCP y UDP en hilos paralelos
        new Thread(this::escucharTCP).start();
        new Thread(this::escucharUDP).start();
        System.out.println("[Nodo " + idNodo + "] Operando de forma distribuida en puertos TCP:" + puertoTCP + " y UDP:" + puertoUDP);
    }

    private void escucharTCP() {
        try (ServerSocket serverSocket = new ServerSocket(puertoTCP)) {
            while (true) {
                Socket clienteAceptado = serverSocket.accept();
                // Reutilizamos tu clase Cliente actual intacta
                Cliente tarea = new Cliente(clienteAceptado, baseDeDatos);
                poolTCP.execute(tarea);
            }
        } catch (IOException e) {
            System.err.println("[Nodo " + idNodo + "] Error en Socket TCP: " + e.getMessage());
        }
    }

    private void escucharUDP() {
        try (DatagramSocket socketPrincipal = new DatagramSocket(puertoUDP)) {
            byte[] reciboBuffer = new byte[1024];
            while (true) {
                DatagramPacket peticion = new DatagramPacket(reciboBuffer, reciboBuffer.length);
                socketPrincipal.receive(peticion);

                String mensaje = new String(peticion.getData(), 0, peticion.getLength());
                if (mensaje.startsWith("PLAY")) {
                    String rutaVideo = mensaje.split(";")[1];
                    // Reutilizamos tu clase Streaming actual intacta
                    Streaming tarea = new Streaming(
                        rutaVideo, 
                        peticion.getAddress(), 
                        peticion.getPort(), 
                        socketPrincipal
                    );
                    poolUDP.execute(tarea);
                }
            }
        } catch (Exception e) {
            System.err.println("[Nodo " + idNodo + "] Error en Socket UDP: " + e.getMessage());
        }
    }

    // Método principal para levantar el nodo desde la terminal
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Uso requerido: java NodoServidor <ID> <PuertoTCP> <PuertoUDP>");
            return;
        }
        int id = Integer.parseInt(args[0]);
        int pTCP = Integer.parseInt(args[1]);
        int pUDP = Integer.parseInt(args[2]);

        NodoServidor nodo = new NodoServidor(id, pTCP, pUDP);
        nodo.iniciar();
    }
}