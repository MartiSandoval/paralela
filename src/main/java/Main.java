import java.io.File;
import java.util.Scanner;

import menusTerminal.MenuInicio;
import player.Reproductor;

public class Main {
    public static void main(String[] args) {
        System.out.println("\033[2J\033[1;1H");
        MenuInicio menu = new MenuInicio();
        Scanner sc = new Scanner(System.in);
        System.out.println("Escribe 'test1' o 'exit' para salir.");
        
        while (true) {
            String ctrl = sc.nextLine();
            if(ctrl.equalsIgnoreCase("test1")) {
                reproducir("src/main/resources/test1.mp4");
            } else if(ctrl.equalsIgnoreCase("exit")) {
                System.out.println("Saliendo...");
                break;
            } else {
                System.out.println("Opción no reconocida. Intente de nuevo.");
            }
        }
        
        sc.close();
    }

    public static void reproducir(String rutaRelativa) {
        // Convierte la ruta relativa a absoluta para que VLC pueda abrirla
        String rutaAbsoluta = new File(rutaRelativa).getAbsolutePath();
        new Thread(() -> Reproductor.lanzar(rutaAbsoluta), "hilo-reproductor").start();
    }
}
