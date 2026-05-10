import java.io.*;
import java.net.*;

public class Streaming implements Runnable {
    private String rutaArchivo;
    private InetAddress ipCliente;
    private int puertoCliente;
    private DatagramSocket socketUDP;

    public Streaming(String ruta, InetAddress ip, int puerto, DatagramSocket socket) {
        this.rutaArchivo = ruta;
        this.ipCliente = ip;
        this.puertoCliente = puerto;
        this.socketUDP = socket;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[64000];
        
        try (FileInputStream fis = new FileInputStream(rutaArchivo)) {
            System.out.println("Enviando video: " + rutaArchivo + " a " + ipCliente);

            int bytesLeidos;
            while ((bytesLeidos = fis.read(buffer)) != -1) {
                DatagramPacket paquete = new DatagramPacket(buffer, bytesLeidos, ipCliente, puertoCliente);
                
                socketUDP.send(paquete);

                Thread.sleep(20); 
            }
            System.out.println("Streaming finalizado para: " + ipCliente);

        } catch (FileNotFoundException e) {
            System.err.println("Error: El archivo de video no existe en el servidor.");
        } catch (Exception e) {
            System.err.println("Error en el flujo de streaming: " + e.getMessage());
        }
    }
}    

