import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String IP_SERVIDOR = "127.0.0.1";
    private static final int PUERTO_UDP_SERVER = 6000;
    private static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        boolean ejecutar = true;
        while (ejecutar) {
            System.out.println("\n=========================================================\r\n" + //
                                "                         Netflix\r\n" + //
                                "=========================================================");
            List<Pelicula> lista = solicitarLista();
            
            if (lista == null) break;

            for (int i = 0; i < lista.size(); i++) {
                System.out.println((i + 1) + ". " + lista.get(i).getTitulo());
            }
            System.out.println("0. Salir");
            
            System.out.print("\nSeleccione una opción: ");
            int op = sc.nextInt();

            if (op == 0) ejecutar = false;
            else if (op > 0 && op <= lista.size()) {
                gestionarDetalle(lista.get(op - 1).getTitulo());
            }
        }
    }

    private static void gestionarDetalle(String titulo) {
        Pelicula p = solicitarInfoPelicula(titulo);

        if (p != null) {
            System.out.println("\n------------------------------");
            System.out.println("TÍTULO: " + p.getTitulo());
            System.out.println("AÑO: " + p.getAño());
            System.out.println("DIRECTORES: " + String.join(", ", p.getDirector()));
            System.out.println("GÉNEROS: " + String.join(", ", p.getGeneros()));
            System.out.println("------------------------------");
            System.out.println("1. Reproducir Película");
            System.out.println("2. Volver al Catálogo");
            System.out.print("Selección: ");
            
            int opcion = sc.nextInt();
            if (opcion == 1) {
                iniciarStreaming(p.getPath());
            }
        }
    }

    private static List<Pelicula> solicitarLista() {
        try (Socket s = new Socket(IP_SERVIDOR, 5000);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            out.writeUTF("SOLICITAR_CATALOGO");
            out.flush();
            return (List<Pelicula>) in.readObject();
        } catch (Exception e) {
            System.err.println("Error al conectar con servidor.");
            return null;
        }
    }

    private static Pelicula solicitarInfoPelicula(String titulo) {
        try (Socket s = new Socket(IP_SERVIDOR, 5000);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            out.writeUTF("VER_DETALLE;" + titulo);
            out.flush();
            return (Pelicula) in.readObject();
        } catch (Exception e) {
            return null;
        }
    }

    private static void iniciarStreaming(String rutaVideo) {
        new Thread(() -> {
            File archivoBuffer = new File("buffer_temporal.mp4");
            
            try (DatagramSocket socketUDP = new DatagramSocket();
                
                 FileOutputStream fos = new FileOutputStream(archivoBuffer)) {
                
                socketUDP.setSoTimeout(3000);

                String mensaje = "PLAY;" + rutaVideo;
                byte[] data = mensaje.getBytes();
                DatagramPacket peticion = new DatagramPacket(data, data.length, 
                                            InetAddress.getByName(IP_SERVIDOR), PUERTO_UDP_SERVER);
                socketUDP.send(peticion);

                System.out.println("Iniciando descarga al búfer...");
                byte[] buffer = new byte[640000];
                int paquetesRecibidos = 0;
                boolean reproductorIniciado = false;
                
                while (true) {
                    DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                    socketUDP.receive(paquete);
                    
                    // Escribimos los bytes recibidos en el archivo
                    fos.write(paquete.getData(), 0, paquete.getLength());
                    fos.flush(); 

                    paquetesRecibidos++;

                    // Pre-buffering: Esperamos 10 paquetes para asegurar que el archivo tenga cabecera
                    if (paquetesRecibidos == 10 && !reproductorIniciado) {
                        System.out.println("Búfer inicial listo. Abriendo reproductor JavaFX...");
                        ReproductorFX.iniciar(archivoBuffer.getAbsolutePath());
                        reproductorIniciado = true;
                    }
                }
            } catch (Exception e) {
                System.err.println("\nFin del streaming UDP o error de red.");
            }
        }).start();
    }
}