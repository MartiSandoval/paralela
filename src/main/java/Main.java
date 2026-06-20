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
    private static final String[][] NODOS_CONOCIDOS = {
        {"127.0.0.1", "5001", "6001"}, // Nodo 1: IP, TCP, UDP
        {"127.0.0.1", "5002", "6002"}, // Nodo 2
        {"127.0.0.1", "5003", "6003"}  // Nodo 3
    };
    private static int nodoActualIndex = 0; 
    private static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        ArrayList<Pelicula> p = solicitarCatalogo();
        if(p == null || p.isEmpty()) {
            System.err.println("Error crítico: Ningún nodo de la red distribuida está disponible.\nSaliendo del sistema...");
            return;
        }

        boolean ejecutar = true;
        while (ejecutar) {
            System.out.println("\n=========================================================");
            System.out.println("                         Netflix Distribuido");
            System.out.println("=========================================================");
            System.out.println("Conectado actualmente al Nodo: " + (nodoActualIndex + 1));
            System.out.println("----------------- Catálogo de películas ----------------");
            for(int i = 0; i < p.size(); i++) {
                Pelicula pel = p.get(i);
                System.out.println((i + 1) + ". " + pel.getTitulo());
            }
            System.out.println("---------------------------------------------------------");
            System.out.println("0. Salir de la aplicación");
            System.out.print("\nSeleccione una opción: ");
            int op = sc.nextInt();
            if (op == 0) { 
                System.out.println("Saliendo del sistema...");
                ejecutar = false; 
            } else if (op > 0 && op <= p.size()) {
                gestionarDetalle(p.get(op - 1).getTitulo());
            }
        }
    }

    private static void gestionarDetalle(String titulo) {
        Pelicula p = solicitarInfoPelicula(titulo);
        if(p == null) {
            System.out.println("No se pudo obtener la información de la película. Todos los nodos fallaron.");
            return;
        }
        
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

    @SuppressWarnings("unchecked")
    private static ArrayList<Pelicula> solicitarCatalogo() {
        int intentos = 0;
        while (intentos < NODOS_CONOCIDOS.length) {
            String ip = NODOS_CONOCIDOS[nodoActualIndex][0];
            int puertoTCP = Integer.parseInt(NODOS_CONOCIDOS[nodoActualIndex][1]);
            
            try(Socket s = new Socket(ip, puertoTCP);
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) {
                out.flush();
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                out.writeUTF("SOLICITAR_CATALOGO");
                out.flush();
                return (ArrayList<Pelicula>) in.readObject();
            } catch (Exception e) {
                System.out.println("[Tolerancia a fallos] Nodo " + (nodoActualIndex + 1) + " no responde. Saltando al siguiente nodo...");
                nodoActualIndex = (nodoActualIndex + 1) % NODOS_CONOCIDOS.length;
                intentos++;
            }
        }
        return null;
    }

    private static Pelicula solicitarInfoPelicula(String titulo) {
        int intentos = 0;
        while (intentos < NODOS_CONOCIDOS.length) {
            String ip = NODOS_CONOCIDOS[nodoActualIndex][0];
            int puertoTCP = Integer.parseInt(NODOS_CONOCIDOS[nodoActualIndex][1]);

            try(Socket s = new Socket(ip, puertoTCP);
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) {
                out.flush();
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                out.writeUTF("VER_DETALLE;" + titulo);
                out.flush();
                return (Pelicula) in.readObject();
            } catch (Exception e) {
                System.out.println("[Tolerancia a fallos] Nodo " + (nodoActualIndex + 1) + " caído al pedir detalles. Rotando...");
                nodoActualIndex = (nodoActualIndex + 1) % NODOS_CONOCIDOS.length;
                intentos++;
            }
        }
        return null;
    }

    private static void iniciarStreaming(String rutaVideo) {
        File archivoBuffer = new File("buffer_temporal.mp4");
        
        // Obtenemos los datos del nodo al que estamos conectados en este momento
        String ip = NODOS_CONOCIDOS[nodoActualIndex][0];
        int puertoUDP = Integer.parseInt(NODOS_CONOCIDOS[nodoActualIndex][2]);
        
        try (DatagramSocket socketUDP = new DatagramSocket();
             FileOutputStream fos = new FileOutputStream(archivoBuffer)) {
            
            socketUDP.setSoTimeout(2000); 

            String mensaje = "PLAY;" + rutaVideo;
            byte[] data = mensaje.getBytes();
            DatagramPacket peticion = new DatagramPacket(data, data.length, InetAddress.getByName(ip), puertoUDP);
            socketUDP.send(peticion);

            System.out.println("Iniciando recepción de datos por UDP desde el Nodo " + (nodoActualIndex + 1) + "...");
            byte[] buffer = new byte[64000];
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
            try { Thread.sleep(500); } catch (InterruptedException ex) {}
            System.out.println("Abriendo el reproductor JavaFX...");
            App.lanzar(archivoBuffer.getAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("\nError crítico en la red UDP: " + e.getMessage());
        }
    }
}