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
        int idNodoTemp = socketCliente.getLocalPort() - 5000; 
        String nombreOrigen = "Nodo " + idNodoTemp; // Lo transformamos a String para el Mensaje
        
        try (ObjectOutputStream out = new ObjectOutputStream(socketCliente.getOutputStream())) {
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socketCliente.getInputStream());
            
            Mensaje peticion = (Mensaje) in.readObject();
            
            NodoServidor.sincronizarReloj(peticion.getRelojLamport(), "Petición recibida: " + peticion.getOperacion(), idNodoTemp);

            Mensaje respuesta = null;

            if (peticion.getOperacion().equals("SOLICITAR_CATALOGO")) {
                NodoServidor.registrarEventoLocal("Empaquetando catálogo de películas", idNodoTemp);
                // AHORA SÍ TIENE 4 PARÁMETROS: (operacion, payload, reloj, idOrigen)
                respuesta = new Mensaje("RESPUESTA_CATALOGO", baseDeDatos.getPeliculas(), NodoServidor.relojLamport.get(), nombreOrigen);
            } 
            else if (peticion.getOperacion().equals("VER_DETALLE")) {
                String titulo = (String) peticion.getPayload(); 
                NodoServidor.registrarEventoLocal("Consultando base de datos para: " + titulo, idNodoTemp);
                Pelicula p = baseDeDatos.getPeliculaPorTitulo(titulo);
                // AHORA SÍ TIENE 4 PARÁMETROS
                respuesta = new Mensaje("RESPUESTA_DETALLE", p, NodoServidor.relojLamport.get(), nombreOrigen);
            }
            
            out.writeObject(respuesta);
            out.flush();

        } catch (Exception e) {
            System.err.println("Error procesando la petición del cliente: " + e.getMessage());
        } finally {
            try { if (socketCliente != null) socketCliente.close(); } catch (Exception e) {}
        }
    }
}