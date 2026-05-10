import java.io.*;
import java.net.*;
import java.util.Scanner;
import menusTerminal.MenuInicio;
import java.util.ArrayList;

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
                menu.mostrar();
                String opcion = sc.nextLine();

                Peticion solicitud = null;

                switch (opcion) {
                    case "1":
                        solicitud = new Peticion("PEDIR_RECOMENDACIONES", null);
                        System.out.println("aaa");
                        Peticion respuestaRecom = (Peticion) in.readObject();
                        System.out.println("abbbb");
                        System.out.println("\n--- TOP 10 RECOMENDACIONES ---\n");
                        ArrayList<Pelicula> lista = (ArrayList<Pelicula>) respuestaRecom.getObjeto();

                        for (int i = 0; i < lista.size(); i++) {
                            System.out.println((i + 1) + "." + lista.get(i).toString());
                        }
                        System.out.println("0. Volver al menú principal");
                        System.out.println("\nSeleccione la pelicual que desea ver: ");
                            
                        try {
                            int num = Integer.parseInt(sc.nextLine());
                            if ((num) > 0 && num <= lista.size()) {
                                Pelicula peli = lista.get(num - 1);
                                    
                                out.writeObject(new Peticion("PEDIR_PELICULA", peli.getNombre()));
                                Peticion resp = (Peticion) in.readObject();
                                System.out.println("\n" + resp.getObjeto() + "\n");
                            } else if (num == 0) {
                                System.out.println("Volviendo al menú principal...");
                            } else {
                                System.out.println("Selección inválida");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Ingrese un número válido");
                        }
                        break;
                    case "2":
                        solicitud = new Peticion("SEGUIR_VIENDO", null); 
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
                                System.out.println("Opción no válida.\n");
                                continue;
                                
                        }
                        
                    case "4":
                        System.out.println("Saliendo del sistema...");
                        ejecutando = false;
                        break;
                    default:
                        System.out.println("Opción no válida. Intente de nuevo.\n");
                        continue;
                }

                if (solicitud != null) {
                    out.writeObject(solicitud);
                    
                    Peticion respuesta = (Peticion) in.readObject();
                    System.out.println("\nRespuesta del servidor: " + respuesta.getObjeto() + "\n");
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error de conexión con el servidor: " + e.getMessage());
        } finally {
            sc.close();
        }
    }
}