import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger; 

public class NodoServidor {
    private final int idNodo;
    private final int puertoTCP;
    private final int puertoUDP;
    private final int puertoCoordinacion;
    private Catalogo baseDeDatos;
    
    private final ExecutorService poolTCP = Executors.newFixedThreadPool(10);
    private final ExecutorService poolUDP = Executors.newFixedThreadPool(5);

    public static final String[][] LISTA_MEMBRESIA = {
        {"1", "127.0.0.1", "5001", "6001"},
        {"2", "127.0.0.1", "5002", "6002"},
        {"3", "127.0.0.1", "5003", "6003"}
    };

    public static AtomicInteger relojLamport = new AtomicInteger(0);

    public static void registrarEventoLocal(String evento, int idNodo) {
        int tiempoActual = relojLamport.incrementAndGet();
        System.out.println("[LAMPORT T=" + tiempoActual + " | Nodo " + idNodo + "] " + evento);
    }

    public static void sincronizarReloj(int relojExterno, String evento, int idNodo) {
        int tiempoActual = relojLamport.updateAndGet(tiempoLocal -> Math.max(tiempoLocal, relojExterno) + 1);
        System.out.println("[LAMPORT T=" + tiempoActual + " | Nodo " + idNodo + "] Sincronización: " + evento);
    }

    public static int coordinadorActual = 3; 
    private boolean esperandoOK = false;
    private long ultimoLatidoCoordinador = System.currentTimeMillis();

    public NodoServidor(int idNodo, int puertoTCP, int puertoUDP) {
        this.idNodo = idNodo;
        this.puertoTCP = puertoTCP;
        this.puertoUDP = puertoUDP;
        this.puertoCoordinacion = puertoUDP + 1000;
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
        
        new Thread(this::escucharCoordinacion).start();
        new Thread(this::monitorCoordinador).start();
        
        System.out.println("[Nodo " + idNodo + "] Operando -> TCP: " + puertoTCP + " | UDP: " + puertoUDP);
        if (this.idNodo == coordinadorActual) {
            System.out.println("==================================================");
            System.out.println("[BULLY] === SOY EL COORDINADOR INICIAL ===");
            System.out.println("==================================================");
        }
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

    private void escucharCoordinacion() {
        try (DatagramSocket socketCord = new DatagramSocket(puertoCoordinacion)) {
            byte[] buffer = new byte[256];
            while (true) {
                DatagramPacket peticion = new DatagramPacket(buffer, buffer.length);
                socketCord.receive(peticion);
                String mensaje = new String(peticion.getData(), 0, peticion.getLength());
                String[] partes = mensaje.trim().split(";");
                String comando = partes[0];
                int idOrigen = Integer.parseInt(partes[1]);

                if (comando.equals("LATIDO")) {
                    if (idOrigen == coordinadorActual) {
                        ultimoLatidoCoordinador = System.currentTimeMillis();
                    }
                } 
                else if (comando.equals("ELECTION")) {
                    System.out.println("[BULLY | Nodo " + idNodo + "] Recibí ELECTION del Nodo " + idOrigen);
                    if (this.idNodo > idOrigen) {
                        enviarMensajeCoordinacion("OK;" + this.idNodo, idOrigen);
                        iniciarEleccion(); 
                    }
                } 
                else if (comando.equals("OK")) {
                    System.out.println("[BULLY | Nodo " + idNodo + "] Recibí OK del Nodo " + idOrigen + ". Me bajo de la contienda.");
                    esperandoOK = false;
                } 
                else if (comando.equals("COORDINADOR")) {
                    coordinadorActual = idOrigen;
                    esperandoOK = false;
                    ultimoLatidoCoordinador = System.currentTimeMillis();
                    System.out.println("==================================================");
                    System.out.println("[BULLY] El NODO " + coordinadorActual + " es el NUEVO COORDINADOR.");
                    System.out.println("==================================================");
                }
                Thread.sleep(5000);// Frecuencia del latido
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
                    System.out.println("[Heartbeat] Nodo " + idNodo + " recibió pulso UDP del Nodo " + idOrigen);
                }
            }
        } catch (Exception e) {
            System.err.println("Error en socket de coordinación: " + e.getMessage());
        }
    }

    private void monitorCoordinador() {
        while (true) {
            try {
                Thread.sleep(2000);
                
                if (this.idNodo == coordinadorActual) {
                    for (String[] nodo : LISTA_MEMBRESIA) {
                        int idDestino = Integer.parseInt(nodo[0]);
                        if (idDestino != this.idNodo) {
                            enviarMensajeCoordinacion("LATIDO;" + this.idNodo, idDestino);
                        }
                    }
                } else {
                    long tiempoInactivo = System.currentTimeMillis() - ultimoLatidoCoordinador;
                    // Timeout de 5 segundos
                    if (tiempoInactivo > 5000 && !esperandoOK) {
                        System.err.println("\n[ALERTA CRÍTICA] ¡TIMEOUT! El Coordinador (Nodo " + coordinadorActual + ") ha caído.");
                        iniciarEleccion();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void iniciarEleccion() {
        System.out.println("[BULLY | Nodo " + idNodo + "] Iniciando proceso de elección...");
        esperandoOK = true;
        boolean soyElMayor = true;

        for (String[] nodo : LISTA_MEMBRESIA) {
            int idDestino = Integer.parseInt(nodo[0]);
            if (idDestino > this.idNodo) {
                soyElMayor = false;
                enviarMensajeCoordinacion("ELECTION;" + this.idNodo, idDestino);
            }
        }

        if (soyElMayor) {
            anunciarVictoria();
        } else {
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    if (esperandoOK) {
                        // Pasó el tiempo y nadie mayor respondió. Asumimos el poder.
                        System.out.println("[BULLY | Nodo " + idNodo + "] Ningún nodo mayor respondió al ELECTION.");
                        anunciarVictoria();
                    }
                } catch (InterruptedException e) {}
            }).start();
        }
    }

    private void anunciarVictoria() {
        coordinadorActual = this.idNodo;
        esperandoOK = false;
        System.out.println("==================================================");
        System.out.println("[BULLY] NUEVO COORDINADOR.");
        System.out.println("==================================================");
        
        // Avisar a todos los nodos que ahora yo mando
        for (String[] nodo : LISTA_MEMBRESIA) {
            int idDestino = Integer.parseInt(nodo[0]);
            if (idDestino != this.idNodo) {
                enviarMensajeCoordinacion("COORDINADOR;" + this.idNodo, idDestino);
            }
        }
    }

    private void enviarMensajeCoordinacion(String msj, int idDestino) {
        try (DatagramSocket socketOut = new DatagramSocket()) {
            byte[] data = msj.getBytes();
            for (String[] nodo : LISTA_MEMBRESIA) {
                if (Integer.parseInt(nodo[0]) == idDestino) {
                    int puertoCordDestino = Integer.parseInt(nodo[3]) + 1000;
                    InetAddress ip = InetAddress.getByName(nodo[1]);
                    DatagramPacket pkt = new DatagramPacket(data, data.length, ip, puertoCordDestino);
                    socketOut.send(pkt);
                    break;
                }
            }
        } catch (Exception e) {
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