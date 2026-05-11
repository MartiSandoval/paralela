import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServidorCatalogo {
    private static final int PUERTO = 5000;
    private static final ExecutorService poolHilos = Executors.newFixedThreadPool(10); 

    public static void main(String[] args) {
        ArrayList<String> datosPeliculas = new ArrayList<>();
        try (InputStream is = ServidorCatalogo.class.getResourceAsStream("/peliculas/lista_peliculas.txt");
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            
            if (is == null) throw new FileNotFoundException("No se encontró el archivo lista_peliculas.txt");

            String linea;
            while ((linea = br.readLine()) != null) {
                if (!linea.trim().isEmpty()) {
                    datosPeliculas.add(linea);
                }
            }
            System.out.println("Base de datos cargada: " + datosPeliculas.size() + " películas encontradas.");

        } catch (Exception e) {
            System.err.println("FALLO CRÍTICO: No se pudo cargar el catálogo. El servidor se cerrará.");
            e.printStackTrace();
            return;
        }

        Catalogo baseDeDatos = new Catalogo(datosPeliculas, datosPeliculas.size());

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor de Catálogo TCP listo en el puerto " + PUERTO);

            while (true) {
                Socket clienteAceptado = serverSocket.accept();
                System.out.println("Cliente conectado: " + clienteAceptado.getInetAddress());

                // Delegamos al manejador concurrente
                Cliente tarea = new Cliente(clienteAceptado, baseDeDatos);
                poolHilos.execute(tarea);
            }
        } catch (IOException e) {
            System.err.println("Error en el socket del servidor: " + e.getMessage());
        }
    }
}