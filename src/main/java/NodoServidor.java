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

    // Lista de membresía mínima para coordinar heartbeats entre nodos
    private static final String[][] LISTA_MEMBRESIA = new String[0][];

    // Utilizamos los mismos pools de hilos que ya tenías en tus servidores por separado
    private final ExecutorService poolTCP = Executors.newFixedThreadPool(10);
    private final ExecutorService poolUDP = Executors.newFixedThreadPool(5);

    private final int puertoHeartbeat;
    private final java.util.Map<Integer, Long> heartbeatsRecibidos = new java.util.HashMap<>();

    public NodoServidor(int idNodo, int puertoTCP, int puertoUDP) {
        this.idNodo = idNodo;
        this.puertoTCP = puertoTCP;
        this.puertoUDP = puertoUDP;
        
        this.puertoHeartbeat = puertoUDP + 1000; 
        
        cargarCatalogo();

        for (String[] nodo : LISTA_MEMBRESIA) {
            int idOtro = Integer.parseInt(nodo[0]);
            if (idOtro != this.idNodo) {
                heartbeatsRecibidos.put(idOtro, System.currentTimeMillis());
            }
        }
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
        new Thread(this::escucharTCP).start();
        new Thread(this::escucharUDP).start();
        new Thread(this::enviarHeartbeats).start();
        new Thread(this::recibirHeartbeats).start();
        new Thread(this::monitorFallos).start();
        
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

    // Envía un ping UDP "HB;MiID" a todos los demás nodos de la membresía cada 2 segundos
    private void enviarHeartbeats() {
        try (DatagramSocket socketHB = new DatagramSocket()) {
            while (true) {
                String mensaje = "HB;" + this.idNodo;
                byte[] data = mensaje.getBytes();
                
                for (String[] nodoInfo : LISTA_MEMBRESIA) {
                    int idDestino = Integer.parseInt(nodoInfo[0]);
                    if (idDestino != this.idNodo) {
                        int puertoDestinoHB = Integer.parseInt(nodoInfo[3]) + 1000;
                        InetAddress ip = InetAddress.getByName(nodoInfo[1]);
                        DatagramPacket pkt = new DatagramPacket(data, data.length, ip, puertoDestinoHB);
                        socketHB.send(pkt);
                    }
                }
                Thread.sleep(2000); // Frecuencia del latido
            }
        } catch (Exception e) {
            System.err.println("[Nodo " + idNodo + "] Error enviando heartbeat: " + e.getMessage());
        }
    }

    // 2. Escucha pings en el puertoHeartbeat y actualiza la estampa de tiempo
    private void recibirHeartbeats() {
        try (DatagramSocket socketEscucha = new DatagramSocket(puertoHeartbeat)) {
            byte[] buffer = new byte[256];
            while (true) {
                DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);
                socketEscucha.receive(pkt);
                String msj = new String(pkt.getData(), 0, pkt.getLength());
                
                if (msj.startsWith("HB;")) {
                    int idOrigen = Integer.parseInt(msj.trim().split(";")[1]);
                    // Actualizamos el reloj de la última vez que vimos vivo a este nodo
                    heartbeatsRecibidos.put(idOrigen, System.currentTimeMillis());
                }
            }
        } catch (Exception e) {
            System.err.println("[Nodo " + idNodo + "] Error recibiendo heartbeat: " + e.getMessage());
        }
    }

    private void monitorFallos() {
        while (true) {
            try {
                Thread.sleep(3000); 
                long tiempoActual = System.currentTimeMillis();
                
                for (Integer idOtroNodo : heartbeatsRecibidos.keySet()) {
                    long ultimoLatido = heartbeatsRecibidos.get(idOtroNodo);
                    
                    if (tiempoActual - ultimoLatido > 5000) { 
                        System.out.println("==============================================");
                        System.out.println("[ALERTA CRÍTICA] ¡TIMEOUT DETECTADO!");
                        System.out.println("[Nodo " + idNodo + "] confirma que el NODO " + idOtroNodo + " ha caído (CRASH).");
                        System.out.println("==============================================");

                        heartbeatsRecibidos.remove(idOtroNodo);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}