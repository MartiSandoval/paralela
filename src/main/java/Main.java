import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    // Membresía conocida por el cliente (Transparencia de ubicación)
    private static final String[][] NODOS_CONOCIDOS = {
        {"127.0.0.1", "5001", "6001"},
        {"127.0.0.1", "5002", "6002"},
        {"127.0.0.1", "5003", "6003"}
    };
    private static int nodoActual = 0; 
    private static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        ArrayList<Pelicula> p = solicitarCatalogo();
        if(p == null || p.isEmpty()) {
            System.err.println("Error crítico: Ningún nodo de la red distribuida está disponible.\nSaliendo...");
            return;
        }

        boolean ejecutar = true;
        while (ejecutar) {
            System.out.println("\n=========================================================");
            System.out.println("                 Netflix Distribuido");
            System.out.println("=========================================================");
            System.out.println("Conectado actualmente al Nodo: " + (nodoActual + 1));
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
        
        System.out.println("\n----- Detalles Pelicula -----");
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
            String ip = NODOS_CONOCIDOS[nodoActual][0];
            int puertoTCP = Integer.parseInt(NODOS_CONOCIDOS[nodoActual][1]);
            
            try(Socket s = new Socket(ip, puertoTCP);
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) {
                out.flush();
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                out.writeUTF("SOLICITAR_CATALOGO");
                out.flush();
                return (ArrayList<Pelicula>) in.readObject();
            } catch (Exception e) {
                System.out.println("[Tolerancia a fallos] Nodo " + (nodoActual + 1) + " no responde. Saltando al siguiente nodo...");
                nodoActual = (nodoActual + 1) % NODOS_CONOCIDOS.length;
                intentos++;
            }
        }
        return null;
    }

    private static Pelicula solicitarInfoPelicula(String titulo) {
        int intentos = 0;
        while (intentos < NODOS_CONOCIDOS.length) {
            String ip = NODOS_CONOCIDOS[nodoActual][0];
            int puertoTCP = Integer.parseInt(NODOS_CONOCIDOS[nodoActual][1]);

            try(Socket s = new Socket(ip, puertoTCP);
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) {
                out.flush();
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                out.writeUTF("VER_DETALLE;" + titulo);
                out.flush();
                return (Pelicula) in.readObject();
            } catch (Exception e) {
                System.out.println("[Tolerancia a fallos] Nodo " + (nodoActual + 1) + " caído al pedir detalles. Rotando...");
                nodoActual = (nodoActual + 1) % NODOS_CONOCIDOS.length;
                intentos++;
            }
        }
        return null;
    }

    private static void iniciarStreaming(String rutaVideo) {
        File archivo = new File("buffer_temporal.mp4");
        String ip = NODOS_CONOCIDOS[nodoActual][0];
        int puertoUDP = Integer.parseInt(NODOS_CONOCIDOS[nodoActual][2]);
        
        try (DatagramSocket socketUDP = new DatagramSocket();
             FileOutputStream fos = new FileOutputStream(archivo)) {
            
            socketUDP.setSoTimeout(2000); 

            byte[] data = ("PLAY;" + rutaVideo).getBytes();
            socketUDP.send(new DatagramPacket(data, data.length, InetAddress.getByName(ip), puertoUDP));

            System.out.println("Iniciando recepción de datos por UDP desde el Nodo " + (nodoActual + 1) + "...");
            byte[] buffer = new byte[64000]; // Ajustado para coincidir con los 64000 de Streaming.java
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
            App.lanzar(archivo.getAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("\nError crítico en la red UDP: " + e.getMessage());
        }
    }
}