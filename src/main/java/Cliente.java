import java.io.*;
import java.net.Socket;
//import java.util.List;

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

            if (peticion.equals("SOLICITAR_CATALOGO")) {
                out.writeObject(baseDeDatos.getPeliculas());
            } 
            else if (peticion.startsWith("VER_DETALLE")) {
                String titulo = peticion.split(";")[1];
                Pelicula p = baseDeDatos.getPeliculaPorTitulo(titulo);
                out.writeObject(p); // Enviamos el objeto individual
            }
            out.flush();

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