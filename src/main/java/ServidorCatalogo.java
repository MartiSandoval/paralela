import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServidorCatalogo {
    public static int miPuerto; 
    private static final ExecutorService poolHilos = Executors.newFixedThreadPool(10); 
    public static int relojLocal = 0;
    
    public static void main(String[] args) {
        // --- NUEVA LÓGICA DE ASIGNACIÓN DE PUERTO ---
        if (args.length > 0) {
            miPuerto = Integer.parseInt(args[0]);
        } else {
            java.util.Scanner sc = new java.util.Scanner(System.in);
            System.out.print("Ingrese el puerto para este nodo (ej. 5000, 5001, 5002): ");
            miPuerto = sc.nextInt();
        }
        
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
        new Thread(new GestorMembresia(miPuerto, "CATALOGO")).start();

        try (ServerSocket serverSocket = new ServerSocket(miPuerto)) {
            System.out.println("Servidor de Catálogo TCP listo en el puerto " + miPuerto);
            
            while (true) {
                Socket clienteAceptado = serverSocket.accept();
                Cliente tarea = new Cliente(clienteAceptado, baseDeDatos);
                poolHilos.execute(tarea);
            }
        } catch (IOException e) {
            System.err.println("Error en el socket del servidor: " + e.getMessage());
        }
        
    }
}