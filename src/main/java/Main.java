import java.util.Scanner;

import menusTerminal.MenuInicio;

public class Main {
    public static void main(String[] args) {
        System.out.println("\033[2J\033[1;1H");
        MenuInicio menu = new MenuInicio();
        Scanner sc = new Scanner(System.in);
        String ctrl = sc.nextLine();
        System.out.println(menu + "\n" + ctrl);
        
        sc.close();
    }
}
