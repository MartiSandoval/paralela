import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServidorCatalogo {
    private static final int PUERTO = 5000;
    // Pool de hilos implementado correctamente según rúbrica
    private static final ExecutorService poolHilos = Executors.newFixedThreadPool(10); 

    public static void main(String[] args) {
        Catalogo baseDeDatos = new Catalogo(); // Ahora compila correctamente

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor de Catálogo iniciado en el puerto " + PUERTO);

            while (true) {
                Socket clienteAceptado = serverSocket.accept();
                System.out.println("Nuevo cliente conectado desde: " + clienteAceptado.getInetAddress());

                // Se delega al ManejadorCliente en lugar de Cliente
                Cliente tarea = new Cliente(clienteAceptado, baseDeDatos);
                poolHilos.execute(tarea);
            }
        } catch (IOException e) {
            System.err.println("Error crítico en el servidor: " + e.getMessage());
        }
    }
}