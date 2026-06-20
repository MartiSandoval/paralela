import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class GestorMembresia implements Runnable {
    // Memoria compartida y segura para hilos: Guarda <Puerto, TipoDeServidor>
    public static ConcurrentHashMap<Integer, String> nodosVivos = new ConcurrentHashMap<>();
    
    private int miPuerto;
    private String miTipo;
    // Rango de puertos donde podrían estar los otros servidores
    private int[] puertosConocidos = {5000, 5001, 5002, 6000, 6001, 6002}; 

    public GestorMembresia(int miPuerto, String miTipo) {
        this.miPuerto = miPuerto;
        this.miTipo = miTipo;
    }

    @Override
    public void run() {
        System.out.println("[" + miTipo + "] Buscando otros nodos en la red...");
        
        for (int puertoDestino : puertosConocidos) {
            if (puertoDestino == miPuerto) continue; // No llamarse a sí mismo

            try (Socket s = new Socket("127.0.0.1", puertoDestino);
                 ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
                
                // 1. Enviar saludo inicial
                Mensaje saludo = new Mensaje("HOLA_SOY_NUEVO", miTipo, 0, miPuerto);
                out.writeObject(saludo);
                out.flush();

                // 2. Recibir respuesta y guardar en el mapa
                Mensaje respuesta = (Mensaje) in.readObject();
                if ("HOLA_RESPUESTA".equals(respuesta.getOperacion())) {
                    String tipoDestino = (String) respuesta.getPayload(); // Desencripta automático
                    nodosVivos.put(puertoDestino, tipoDestino);
                    System.out.println("-> [Membresía] Nodo descubierto: " + tipoDestino + " en puerto " + puertoDestino);
                }

            } catch (Exception e) {
                // Si entra aquí, el puerto está inactivo. Se ignora silenciosamente.
            }
        }
        System.out.println("Topología actual conocida: " + nodosVivos.toString());
    }
}