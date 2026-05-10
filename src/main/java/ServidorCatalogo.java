import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServidorCatalogo {
    private static final int PUERTO = 5000;
    // Creamos un Pool de 10 hilos fijos
    private static final ExecutorService poolHilos = Executors.newFixedThreadPool(10); 

    public static void main(String[] args) {
        Catalogo baseDeDatos = new Catalogo();

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor de Catálogo iniciado en el puerto " + PUERTO);

            // Bucle infinito para escuchar continuamente a nuevos clientes
            while (true) {
                Socket clienteAceptado = serverSocket.accept();
                System.out.println("Nuevo cliente conectado desde: " + clienteAceptado.getInetAddress());

                // En lugar de new Thread(...).start(), le pasamos la tarea al Pool
                Cliente tarea = new Cliente(clienteAceptado, baseDeDatos);
                poolHilos.execute(tarea);
            }
        } catch (IOException e) {
            System.err.println("Error crítico en el servidor: " + e.getMessage());
        }
    }
}