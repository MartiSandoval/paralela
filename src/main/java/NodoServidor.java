import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger; 

public class NodoServidor {
    private final int idNodo;
    private final int puertoTCP;
    private final int puertoUDP;
    private final int puertoCoordinacion; 
    private Catalogo baseDeDatos;
    
    // Soportamos concurrencia masiva para la Sección 3
    private final ExecutorService poolTCP = Executors.newFixedThreadPool(50);
    private final ExecutorService poolUDP = Executors.newFixedThreadPool(5);

    public static final String[][] LISTA_MEMBRESIA = {
        {"1", "127.0.0.1", "5001", "6001"},
        {"2", "127.0.0.1", "5002", "6002"},
        {"3", "127.0.0.1", "5003", "6003"}
    };

    public static AtomicInteger relojLamport = new AtomicInteger(0);
    private final AtomicInteger contadorLatidosRecibidos = new AtomicInteger(0);

    public static void registrarEventoLocal(String evento, int idNodo) {
        int tiempoActual = relojLamport.incrementAndGet();
        // Solo imprimimos si no estamos saturados de carga para no frenar la consola
        if (tiempoActual % 100 == 0) System.out.println("[LAMPORT T=" + tiempoActual + " | Nodo " + idNodo + "] " + evento + "\n");
    }

    public static void sincronizarReloj(int relojExterno, String evento, int idNodo) {
        relojLamport.updateAndGet(tiempoLocal -> Math.max(tiempoLocal, relojExterno) + 1);
    }

    // --- ALGORITMO BULLY ---
    // No se asume ningun coordinador de antemano: arranca en 0 (ningun ID
    // real de nodo es 0, asi que cualquier nodo activo "le gana" a este
    // valor inicial hasta que la primera eleccion real lo reemplace).
    public static int coordinadorActual = 0;
    private volatile boolean esperandoOK = false;
    private volatile boolean eleccionEnCurso = false;
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
            String linea;
            while ((linea = br.readLine()) != null) {
                if (!linea.trim().isEmpty()) datosPeliculas.add(linea);
            }
            this.baseDeDatos = new Catalogo(datosPeliculas, datosPeliculas.size());
            System.out.println("[Nodo " + idNodo + "] Catálogo cargado: " + datosPeliculas.size() + " películas.");
        } catch (Exception e) {
            System.err.println("[Nodo " + idNodo + "] Error al cargar catálogo.");
        }
    }

    public void iniciar() {
        new Thread(this::escucharTCP).start();
        new Thread(this::escucharUDP).start();
        new Thread(this::escucharCoordinacion).start();
        new Thread(this::monitorCoordinador).start();

        System.out.println("[Nodo " + idNodo + "] Operando -> TCP: " + puertoTCP + " | UDP: " + puertoUDP);

        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            iniciarEleccion();
        }).start();
    }

    private void escucharTCP() {
        try (ServerSocket serverSocket = new ServerSocket(puertoTCP)) {
            while (true) {
                Socket cliente = serverSocket.accept();
                poolTCP.execute(new Cliente(cliente, baseDeDatos));
            }
        } catch (IOException e) {}
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
        } catch (Exception e) {}
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

                if (comando.equals("LATIDO") && idOrigen == coordinadorActual) {
                    ultimoLatidoCoordinador = System.currentTimeMillis();
                    if (contadorLatidosRecibidos.incrementAndGet() % 5 == 0) {
                        System.out.println("[BULLY | Nodo " + idNodo + "] Latido recibido de Nodo " + idOrigen);
                    }
                } 
                else if (comando.equals("ELECTION")) {
                    if (this.idNodo > idOrigen) {
                        enviarMensajeCoordinacion("OK;" + this.idNodo, idOrigen);
                        if (!eleccionEnCurso) {
                            iniciarEleccion();
                        }
                    }
                } 
                else if (comando.equals("OK")) {
                    manejarOkRecibido(idOrigen);
                } 
                else if (comando.equals("COORDINADOR")) {
                    manejarCoordinadorRecibido(idOrigen);
                }
            }
        } catch (Exception e) {}
    }

    private synchronized void manejarOkRecibido(int idOrigen) {
        esperandoOK = false;
    }

    private synchronized void manejarCoordinadorRecibido(int idOrigen) {
        coordinadorActual = idOrigen;
        esperandoOK = false;
        eleccionEnCurso = false;
        ultimoLatidoCoordinador = System.currentTimeMillis();
        System.out.println("[BULLY] El NODO " + coordinadorActual + " es el NUEVO COORDINADOR.");
    }

    private void monitorCoordinador() {
        int contadorCiclos = 0;
        while (true) {
            try {
                Thread.sleep(2000);
                contadorCiclos++;
                boolean tocaLoguear = (contadorCiclos % 5 == 0);

                if (this.idNodo == coordinadorActual) {
                    for (String[] nodo : LISTA_MEMBRESIA) {
                        int idDestino = Integer.parseInt(nodo[0]);
                        if (idDestino != this.idNodo) {
                            enviarMensajeCoordinacion("LATIDO;" + this.idNodo, idDestino);
                            if (tocaLoguear) {
                                System.out.println("[BULLY | Nodo " + idNodo + "] Latido enviado a Nodo " + idDestino);
                            }
                        }
                    }
                } else {
                    long tiempoInactivo = System.currentTimeMillis() - ultimoLatidoCoordinador;
                    if (eleccionEnCurso) {
                        if (tocaLoguear) {
                            System.out.println("[BULLY | Nodo " + idNodo + "] Eleccion en curso, coordinador " + coordinadorActual + " ya no es valido.");
                        }
                    } else {
                        if (tocaLoguear && tiempoInactivo > 3000) {
                            System.out.println("[BULLY | Nodo " + idNodo + "] Esperando latido de Nodo " + coordinadorActual
                                + " (ultimo hace " + tiempoInactivo + " ms)");
                        }
                        if (tiempoInactivo > 5000 && !esperandoOK) {
                            System.err.println("[¡ALERTA!] ¡TIMEOUT! Coordinador " + coordinadorActual + " ha caído.");
                            iniciarEleccion();
                        }
                    }
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    private synchronized void iniciarEleccion() {
        System.out.println("[BULLY | Nodo " + idNodo + "] Iniciando elección...");
        eleccionEnCurso = true;
        esperandoOK = true;
        boolean soyElMayor = true;

        for (String[] nodo : LISTA_MEMBRESIA) {
            int idDestino = Integer.parseInt(nodo[0]);
            if (idDestino > this.idNodo) {
                soyElMayor = false;
                enviarMensajeCoordinacion("ELECTION;" + this.idNodo, idDestino);
            }
        }

        if (soyElMayor) anunciarVictoria();
        else {
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    if (esperandoOK) anunciarVictoria();
                } catch (InterruptedException e) {}
            }).start();
        }
    }

    private synchronized void anunciarVictoria() {
        coordinadorActual = this.idNodo;
        esperandoOK = false;
        eleccionEnCurso = false;
        System.out.println("[BULLY] SOY EL NUEVO COORDINADOR.");
        for (String[] nodo : LISTA_MEMBRESIA) {
            int idDestino = Integer.parseInt(nodo[0]);
            if (idDestino != this.idNodo) enviarMensajeCoordinacion("COORDINADOR;" + this.idNodo, idDestino);
        }
    }

    private void enviarMensajeCoordinacion(String msj, int idDestino) {
        try (DatagramSocket socketOut = new DatagramSocket()) {
            byte[] data = msj.getBytes();
            for (String[] nodo : LISTA_MEMBRESIA) {
                if (Integer.parseInt(nodo[0]) == idDestino) {
                    int pDestino = Integer.parseInt(nodo[3]) + 1000;
                    socketOut.send(new DatagramPacket(data, data.length, InetAddress.getByName(nodo[1]), pDestino));
                    break;
                }
            }
        } catch (Exception e) {}
    }

    public static void main(String[] args) {
        if (args.length < 3) return;
        new NodoServidor(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2])).iniciar();
    }
}