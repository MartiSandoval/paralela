import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;


public class Servidor {
    // Esto del pool de hilos la verdad es q no lo entiendo mucho... pero funciona todo
    private static final int PUERTO = 5000;
    private static final ExecutorService poolHilos = Executors.newFixedThreadPool(10); 

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor Netflix iniciado en el puerto " + PUERTO);

            while (true) {
                Socket clienteSocket = serverSocket.accept();
                System.out.println("Nuevo cliente conectado: " + clienteSocket.getInetAddress());
                
                poolHilos.execute(new ManejadorCliente(clienteSocket));
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }
}

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
            Peticion mensajeRecibido = (Peticion) in.readObject();
            System.out.println("Acción solicitada: " + mensajeRecibido.getAccion());

            if ("PEDIR_RECOMENDACIONES".equals(mensajeRecibido.getAccion())) {
                ArrayList<Pelicula> recomendadas = new ArrayList<>();
                recomendadas.add(new Pelicula("Minecraft", "yo", 2025));
                recomendadas.add(new Pelicula("Interstellar", "Christopher Nolan", 2014));
                recomendadas.add(new Pelicula("Shrek", "Andrew Adamson", 2001));
    
    // Enviamos el objeto complejo directamente (Marshalling)
                out.writeObject(new Peticion("RESPUESTA_RECOMENDACIONES", recomendadas));

            } else if ("PEDIR_PELICULA".equals(mensajeRecibido.getAccion())) {
                String nombrePelicula = (String) mensajeRecibido.getObjeto();
    
                out.writeObject(new Peticion("REPRODUCCION_INICIADA", "Reproduciendo archivo de: " + nombrePelicula + "..."));
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error en la comunicación con el cliente: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException e) {}
        }
    }
}