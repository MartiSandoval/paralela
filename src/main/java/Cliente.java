import java.io.*;
import java.net.Socket;

public class Cliente implements Runnable {
    private Socket socketCliente;
    private Catalogo baseDeDatos;

    public Cliente(Socket socket, Catalogo bd) {
        this.socketCliente = socket;
        this.baseDeDatos = bd;
    }

    @Override
    public void run() {
        try (ObjectOutputStream out = new ObjectOutputStream(socketCliente.getOutputStream())) {
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socketCliente.getInputStream());
            
            String peticion = in.readUTF();
            System.out.println("Petición recibida del cliente: " + peticion);

            if (peticion.equals("SOLICITAR_CATALOGO")) {
                // Aquí enviamos la copia limpia que acabamos de arreglar
                out.writeObject(baseDeDatos.getPeliculas());
            } 
            else if (peticion.startsWith("VER_DETALLE")) {
                String titulo = peticion.split(";")[1];
                Pelicula p = baseDeDatos.getPeliculaPorTitulo(titulo);
                out.writeObject(p);
            }
            out.flush();

        } catch (Exception e) {
            System.err.println("Error procesando la petición del cliente:");
            e.printStackTrace(); 
        } finally {
            try {
                if (socketCliente != null && !socketCliente.isClosed()) {
                    socketCliente.close();
                }
            } catch (IOException e) {
                System.err.println("Error cerrando socket: " + e.getMessage());
            }
        }
    }
}