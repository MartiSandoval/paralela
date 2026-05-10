import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String IP_SERVIDOR = "127.0.0.1";
    private static final int PUERTO_TCP = 5000;
    private static final int PUERTO_UDP_SERVER = 6000;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        
        List<Pelicula> catalogo = solicitarCatalogo();
        
        if (catalogo == null || catalogo.isEmpty()) {
            System.err.println("No se pudo cargar el catálogo. Verifique el servidor.");
            return;
        }

        System.out.println("\n=========================================================\r\n" + //
                        "                         Netflix\r\n" + //
                        "=========================================================\n");
        for (int i = 0; i < catalogo.size(); i++) {
            System.out.println((i + 1) + ". " + catalogo.get(i).getTitulo());
        }
        System.out.println("\n0. Salir de la aplicación");
        System.out.print("\nSeleccione el número de la película para reproducir: ");
        int seleccion = sc.nextInt();
        
        if (seleccion >= 0 && seleccion < catalogo.size()) {
            Pelicula elegida = catalogo.get(seleccion);
            iniciarStreaming(elegida.getPath());
        }
    }

    private static List<Pelicula> solicitarCatalogo() {
        try (Socket socket = new Socket(IP_SERVIDOR, PUERTO_TCP);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeUTF("SOLICITAR_CATALOGO");
            out.flush();
            return (List<Pelicula>) in.readObject();

        } catch (Exception e) {
            System.err.println("Fallo de Transparencia/Acceso: " + e.getMessage());
            return null;
        }
    }

    private static void iniciarStreaming(String rutaVideo) {
        new Thread(() -> {
            try (DatagramSocket socketUDP = new DatagramSocket()) {

                String mensaje = "PLAY;" + rutaVideo;
                byte[] data = mensaje.getBytes();
                DatagramPacket peticion = new DatagramPacket(data, data.length, 
                                            InetAddress.getByName(IP_SERVIDOR), PUERTO_UDP_SERVER);
                socketUDP.send(peticion);

                System.out.println("Reproduciendo: " + rutaVideo + "...");
                byte[] buffer = new byte[64000];
                
                while (true) {
                    DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                    socketUDP.receive(paquete);
                    System.out.print(".");
                }
            } catch (Exception e) {
                System.err.println("\nFin del streaming o error de red.");
            }
        }).start();
    }
}