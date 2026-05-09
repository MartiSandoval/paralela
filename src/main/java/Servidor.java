import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Servidor {
    private static final int PUERTO = 5000;
    // Pool de hilos para manejar múltiples clientes concurrentes
    private static final ExecutorService poolHilos = Executors.newFixedThreadPool(10); 

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor Netflix iniciado en el puerto " + PUERTO + "...");

            while (true) {
                Socket clienteSocket = serverSocket.accept();
                System.out.println("Nuevo cliente conectado: " + clienteSocket.getInetAddress());
                
                // Derivamos el cliente a un hilo independiente
                poolHilos.execute(new ManejadorCliente(clienteSocket));
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }
}

// Clase interna o externa para manejar la lógica de cada cliente
class ManejadorCliente implements Runnable {
    private Socket socket;

    public ManejadorCliente(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            // Leer mensaje del cliente
            Peticion mensajeRecibido = (Peticion) in.readObject();
            System.out.println("Acción solicitada: " + mensajeRecibido.getAccion());

            // Lógica basada en los diagramas de secuencia
            if ("PEDIR_RECOMENDACIONES".equals(mensajeRecibido.getAccion())) {
                // Aquí iría tu lógica de la clase Catalogo
                String respuesta = "Catálogo: [1. Matrix, 2. Shrek, 3. Inception]";
                out.writeObject(new Peticion("RESPUESTA_CATALOGO", respuesta));
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error en la comunicación con el cliente: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException e) { /* Ignorar */ }
        }
    }
}