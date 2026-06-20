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
        try(ObjectOutputStream out = new ObjectOutputStream(socketCliente.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socketCliente.getInputStream())) {
            
            // 1. Leemos el objeto Mensaje en lugar del String
            Mensaje peticion = (Mensaje) in.readObject();
            
            // 2. Sincronizamos el reloj del servidor
            ServidorCatalogo.relojLocal = Math.max(ServidorCatalogo.relojLocal, peticion.getRelojLamport()) + 1;
            
            System.out.println("Log [" + ServidorCatalogo.relojLocal + "]: Petición recibida -> " + peticion.getOperacion());
            // --- NUEVO: Procesar saludos de la red (Membresía) ---
            if (peticion.getOperacion().equals("HOLA_SOY_NUEVO")) {
                int puertoHermano = peticion.getPuertoOrigen();
                String tipoHermano = (String) peticion.getPayload(); // Ej: "VIDEO" o "CATALOGO"
                
                // Guardo a mi hermano en la memoria
                GestorMembresia.nodosVivos.put(puertoHermano, tipoHermano);
                System.out.println("[Red] Se unió un nodo " + tipoHermano + " en el puerto " + puertoHermano);
                
                // Le envío mi información para que él también me guarde
                Mensaje miRespuesta = new Mensaje("HOLA_RESPUESTA", "CATALOGO", ServidorCatalogo.relojLocal, ServidorCatalogo.miPuerto);
                out.writeObject(miRespuesta);
                out.flush();
                return; // Termina la ejecución de este hilo porque solo era un saludo
            }

            // 3. Procesamos y respondemos empaquetando los datos
            if (peticion.getOperacion().equals("SOLICITAR_CATALOGO")) {
                ServidorCatalogo.relojLocal++;
                Mensaje respuesta = new Mensaje("RESPUESTA_CATALOGO", baseDeDatos.getPeliculas(), ServidorCatalogo.relojLocal, 5000);
                out.writeObject(respuesta);
            } 
            else if (peticion.getOperacion().equals("VER_DETALLE")) {
                String titulo = (String) peticion.getPayload(); // Sacamos el String que envió el cliente
                Pelicula p = baseDeDatos.getPeliculaPorTitulo(titulo);
                
                ServidorCatalogo.relojLocal++;
                Mensaje respuesta = new Mensaje("RESPUESTA_DETALLE", p, ServidorCatalogo.relojLocal, 5000);
                out.writeObject(respuesta);
            }
            else if (peticion.getOperacion().equals("HEARTBEAT")) {
                // Le enviamos un acuse de recibo para un cierre limpio de TCP
                Mensaje ack = new Mensaje("ACK", null, ServidorCatalogo.relojLocal, ServidorCatalogo.miPuerto);
                out.writeObject(ack);
                out.flush();
                return; // Cortamos la ejecución aquí limpiamente
            }
            out.flush();

        } catch (Exception e) { // Cambiar IOException a Exception por el readObject
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