import java.io.*;
import java.net.*;
import java.util.Scanner;

import menusTerminal.MenuInicio;

public class Cliente {
    private static final int PUERTO = 5000;

    public static void main(String[] args) {
        System.out.print("\033[2J\033[1;1H"); 
        
        MenuInicio menu = new MenuInicio();
        Scanner sc = new Scanner(System.in);

        try (Socket socket = new Socket("localhost", PUERTO);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            System.out.println("Conectado al servidor Netflix...");
            
            boolean ejecutando = true;

            while (ejecutando) {
                menu.mostrarInicio();
                String opcion = sc.nextLine();

                Peticion solicitud = null;

                switch (opcion) {
                    case "1":
                        solicitud = new Peticion("PEDIR_RECOMENDACIONES", null);
                        break;
                    case "2":
                        solicitud = new Peticion("SEGUIR_VIENDO", null); // Requeriría enviar el ID del usuario
                        break;
                    case "3":
                        menu.mostrarBusqueda();
                        String opcion2 = sc.nextLine();
                        switch (opcion2) {
                            case "1":
                                System.out.print("Ingrese término de búsqueda: ");
                                String filtro = sc.nextLine();
                                solicitud = new Peticion("BUSCAR_PELICULA", filtro);
                                break;
                            case "2": //búsqueda
                                System.out.println("Nombre pelicula");
                                String nombre = sc.nextLine();
                                solicitud = new Peticion("PELICULA", nombre);
                                break;
                            case "3": // volver atrás
                                continue;
                            default:
                                System.out.println("❌ Opción no válida. Intente de nuevo.\n");
                                continue;
                                
                        }
                        
                    case "4":
                        System.out.println("Saliendo del sistema...");
                        ejecutando = false;
                        break;
                    default:
                        System.out.println("❌ Opción no válida. Intente de nuevo.\n");
                        continue;
                }

                if (solicitud != null) {
                    // Enviar petición al servidor
                    out.writeObject(solicitud);
                    
                    // Esperar y leer la respuesta
                    Peticion respuesta = (Peticion) in.readObject();
                    System.out.println("\n🎬 Respuesta del servidor: " + respuesta.getObjeto() + "\n");
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("❌ Error de conexión con el servidor: " + e.getMessage());
        } finally {
            sc.close();
        }
    }
}