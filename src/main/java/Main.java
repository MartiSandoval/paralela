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
    private static final int[] PUERTOS_CATALOGO = {5000, 5001, 5002}; 
    public static int relojCliente = 0; 
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
        // 1. Avanza el reloj antes del evento de envío
        relojCliente++; 

        // 2. Bucle de tolerancia a fallos (recorre los puertos 5000, 5001...)
        for (int puertoDestino : PUERTOS_CATALOGO) {
            try (Socket s = new Socket("127.0.0.1", puertoDestino);
                 ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
                
                // 3. Instrucción correcta y envío del título protegido
                Mensaje msjEnvio = new Mensaje("VER_DETALLE", titulo, relojCliente, 0);
                out.writeObject(msjEnvio);
                out.flush();
                
                // 4. Recibimos el "Sobre" del servidor
                Mensaje msjRecibido = (Mensaje) in.readObject();
                
                // 5. Actualizamos el reloj lógico: max(local, recibido) + 1
                relojCliente = Math.max(relojCliente, msjRecibido.getRelojLamport()) + 1;
                
                // 6. Extraemos la película desencriptada del sobre
                return (Pelicula) msjRecibido.getPayload();

            } catch (Exception e) {
                System.out.println("Nodo Catálogo en puerto " + puertoDestino + " no responde. Intentando con nodo de respaldo...");
                // Continúa el for() intentando con el siguiente puerto
            }
        }
        
        System.err.println("Error crítico: Todos los nodos del catálogo están caídos.");
        return null;
    }

    @SuppressWarnings("unchecked")
    private static ArrayList<Pelicula> solicitarCatalogo() {
        relojCliente++; 

        for (int puertoDestino : PUERTOS_CATALOGO) {
            try (Socket s = new Socket(IP_SERVIDOR, puertoDestino);
                 ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
                
                Mensaje msjEnvio = new Mensaje("SOLICITAR_CATALOGO", null, relojCliente, 0);
                out.writeObject(msjEnvio);
                out.flush();
                
                Mensaje msjRecibido = (Mensaje) in.readObject();
                
                relojCliente = Math.max(relojCliente, msjRecibido.getRelojLamport()) + 1;
                
                return (ArrayList<Pelicula>) msjRecibido.getPayload(); 
                
            } catch (Exception e) {
                System.out.println("Nodo Catálogo " + puertoDestino + " no responde al arranque. Buscando otro...");
                // Falla controlada, intenta con el siguiente.
            }
        }
        return null; // Solo retorna null si TODOS los nodos están caídos
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
