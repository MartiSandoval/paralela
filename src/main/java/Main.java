import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    private static final String IP_SERVIDOR = "127.0.0.1";
    private static final int PUERTO_UDP_SERVER = 6000;
    private static Scanner sc = new Scanner(System.in);
    public static void main(String[] args) {
        ArrayList<Pelicula> p = solicitarCatalogo();
        if(p == null || p.isEmpty()) {
            System.err.println("No se pudo conectar a servidorCatalogo\nSaliendo del sistema...");
            return;
        }

        boolean ejecutar = true;
        while (ejecutar) {
            System.out.println("\n=========================================================");
            System.out.println("                         Netflix");
            System.out.println("=========================================================");
            
            System.out.println("----------------- Catálogo de películas ----------------");
            for(int i = 0; i < p.size(); i++) {
                Pelicula pel = p.get(i);
                System.out.println((i + 1) + ". " + pel.titulo);
            }
            System.out.println("---------------------------------------------------------");
            System.out.println("0. Salir de la aplicación");
            System.out.print("\nSeleccione una opción: ");
            int op = sc.nextInt();
            if (op == 0) { 
                System.out.println("Saliendo del sistema...");
                ejecutar = false; 
            } else if (op > 0 && op <= p.size()) {
                System.out.println(p.get(op - 1).getTitulo());
                gestionarDetalle(p.get(op - 1).getTitulo());
            }
        }
    }

    private static void gestionarDetalle(String titulo) {
        Pelicula p = solicitarInfoPelicula(titulo);
        if(p==null) {
            System.out.println("No se pudo obtener la información de la película.");
            return;
        }
        if (p != null) {
            System.out.println("\n-----Detalles Pelicula-----");
            System.out.println("TÍTULO: " + p.getTitulo());
            System.out.println("AÑO: " + p.getAño());
            System.out.println("DIRECTORES: " + String.join(", ", p.getDirector()));
            System.out.println("GÉNEROS: " + String.join(", ", p.getGeneros()));
            System.out.println("---------------------------\n");
            System.out.println("1. Reproducir película");
            System.out.println("2. Volver al catálogo");
            System.out.print("Seleccione una opción: ");
            
            int opcion = sc.nextInt();
            if (opcion == 1) {
                System.out.println("Reproduciendo: " + p.getTitulo());
                iniciarStreaming(p.getPath());
            }
        }
    }

    private static Pelicula solicitarInfoPelicula(String titulo) {
        try(Socket s = new Socket(IP_SERVIDOR, 5000);
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) {
            out.flush();
            ObjectInputStream in = new ObjectInputStream(s.getInputStream());
            out.writeUTF("VER_DETALLE;" + titulo);
            out.flush();
            
            return (Pelicula) in.readObject();
        } catch (Exception e) {
            System.err.println("Error al solicitar información de la película: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static ArrayList<Pelicula> solicitarCatalogo() {
        try(Socket s = new Socket(IP_SERVIDOR, 5000);
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) {
            out.flush();
            ObjectInputStream in = new ObjectInputStream(s.getInputStream());
            out.writeUTF("SOLICITAR_CATALOGO");
            out.flush();
            return (ArrayList<Pelicula>) in.readObject();
        } catch (Exception e) {
            return null;
        }
    }

    private static void iniciarStreaming(String rutaVideo) {
        File archivoBuffer = new File("buffer_temporal.mp4");
        
        try (DatagramSocket socketUDP = new DatagramSocket();
             FileOutputStream fos = new FileOutputStream(archivoBuffer)) {
            
            socketUDP.setSoTimeout(2000); 

            String mensaje = "PLAY;" + rutaVideo;
            byte[] data = mensaje.getBytes();
            DatagramPacket peticion = new DatagramPacket(data, data.length, 
                                        InetAddress.getByName(IP_SERVIDOR), PUERTO_UDP_SERVER);
            socketUDP.send(peticion);

            System.out.println("Iniciando recepción de datos por UDP...");
            byte[] buffer = new byte[640000];
            int paquetesRecibidos = 0;
            
            while (true) {
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                socketUDP.receive(paquete);
                
                fos.write(paquete.getData(), 0, paquete.getLength());
                fos.flush(); 

                paquetesRecibidos++;
                if (paquetesRecibidos % 50 == 0) {
                    System.out.println("Recibiendo fragmentos... (" + paquetesRecibidos + " paquetes)");
                }
            }
            
        } catch (java.net.SocketTimeoutException e) {
            System.out.println("\nTransferencia completada. Guardando archivo en disco...");
            
            try {
                Thread.sleep(500); 
            } catch (InterruptedException ex) {}

            System.out.println("Abriendo el reproductor JavaFX...");
            App.lanzar(archivoBuffer.getAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("\nError crítico en la red UDP: " + e.getMessage());
        }
    }
}
