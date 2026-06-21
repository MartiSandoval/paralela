import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger; // IMPORTANTE PARA EL RELOJ

public class NodoServidor {
    private final int idNodo;
    private final int puertoTCP;
    private final int puertoUDP;
    private Catalogo baseDeDatos;
    
    private final ExecutorService poolTCP = Executors.newFixedThreadPool(10);
    private final ExecutorService poolUDP = Executors.newFixedThreadPool(5);

    // Lista de membresía estática (Requisito 2.1)
    public static final String[][] LISTA_MEMBRESIA = {
        {"1", "127.0.0.1", "5001", "6001"},
        {"2", "127.0.0.1", "5002", "6002"},
        {"3", "127.0.0.1", "5003", "6003"}
    };

    // --- LÓGICA DEL RELOJ DE LAMPORT (REQUISITO 2.2) ---
    public static AtomicInteger relojLamport = new AtomicInteger(0);

    public static void registrarEventoLocal(String evento, int idNodo) {
        int tiempoActual = relojLamport.incrementAndGet();
        System.out.println("[LAMPORT T=" + tiempoActual + " | Nodo " + idNodo + "] " + evento);
    }

    public static void sincronizarReloj(int relojExterno, String evento, int idNodo) {
        int tiempoActual = relojLamport.updateAndGet(tiempoLocal -> Math.max(tiempoLocal, relojExterno) + 1);
        System.out.println("[LAMPORT T=" + tiempoActual + " | Nodo " + idNodo + "] Sincronización: " + evento);
    }
    // ----------------------------------------------------

    public NodoServidor(int idNodo, int puertoTCP, int puertoUDP) {
        this.idNodo = idNodo;
        this.puertoTCP = puertoTCP;
        this.puertoUDP = puertoUDP;
        cargarCatalogo();
    }

    private void cargarCatalogo() {
        ArrayList<String> datosPeliculas = new ArrayList<>();
        try (InputStream is = NodoServidor.class.getResourceAsStream("/peliculas/lista_peliculas.txt");
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            if (is == null) throw new FileNotFoundException("No se encontró lista_peliculas.txt");
            String linea;
            while ((linea = br.readLine()) != null) {
                if (!linea.trim().isEmpty()) datosPeliculas.add(linea);
            }
            this.baseDeDatos = new Catalogo(datosPeliculas, datosPeliculas.size());
            System.out.println("[Nodo " + idNodo + "] Catálogo cargado: " + datosPeliculas.size() + " películas.");
        } catch (Exception e) {
            System.err.println("[Nodo " + idNodo + "] Error al cargar catálogo: " + e.getMessage());
        }
    }

    public void iniciar() {
        new Thread(this::escucharTCP).start();
        new Thread(this::escucharUDP).start();
        System.out.println("[Nodo " + idNodo + "] Operando -> TCP: " + puertoTCP + " | UDP: " + puertoUDP);
    }

    private void escucharTCP() {
        try (ServerSocket serverSocket = new ServerSocket(puertoTCP)) {
            while (true) {
                Socket cliente = serverSocket.accept();
                poolTCP.execute(new Cliente(cliente, baseDeDatos));
            }
        } catch (IOException e) {
            System.err.println("Error TCP: " + e.getMessage());
        }
    }

    private void escucharUDP() {
        try (DatagramSocket socketUDP = new DatagramSocket(puertoUDP)) {
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket peticion = new DatagramPacket(buffer, buffer.length);
                socketUDP.receive(peticion);
                String msj = new String(peticion.getData(), 0, peticion.getLength());
                if (msj.startsWith("PLAY")) {
                    String ruta = msj.split(";")[1];
                    poolUDP.execute(new Streaming(ruta, peticion.getAddress(), peticion.getPort(), socketUDP));
                }
            }
        } catch (Exception e) {
            System.err.println("Error UDP: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Uso: java NodoServidor <ID> <PuertoTCP> <PuertoUDP>");
            return;
        }
        new NodoServidor(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2])).iniciar();
    }
}