import java.io.*;
import java.net.Socket;
import java.util.List;

public class Cliente implements Runnable {
    private Socket socketCliente;
    private Catalogo baseDeDatos;

    public Cliente(Socket socket, Catalogo bd) {
        this.socketCliente = socket;
        this.baseDeDatos = bd;
    }

    @Override
    public void run() {
        try (
            ObjectOutputStream out = new ObjectOutputStream(socketCliente.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socketCliente.getInputStream())
        ) {
            String peticion = in.readUTF();
            System.out.println("Petición recibida: " + peticion);

            if ("SOLICITAR_CATALOGO".equals(peticion)) {
                // Aquí llama al método protegido que creamos en Catalogo
                List<Pelicula> respuesta = baseDeDatos.getPeliculas();
                
                // Envío por red (Marshalling)
                out.writeObject(respuesta);
                out.flush();
            }

        } catch (IOException e) {
            System.err.println("Fallo de conexión con el cliente: " + e.getMessage());
        } finally {
            try {
                socketCliente.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}