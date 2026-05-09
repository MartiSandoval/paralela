package menusTerminal;

public class MenuInicio {
    public MenuInicio() {
        System.out.println("===============Netflix===============");
        System.out.println("Seleccione una opción:");
        System.out.println("1. Ver recomendaciones");
        System.out.println("2. Seguir viendo");
        System.out.println("3. Buscar pelicula"); // dentro de esta opción estará filtrar
        System.out.println("4. Salir del sistema");
    }

    public static void main(String[] args) {
        MenuInicio menu = new MenuInicio();
        System.out.println(menu);
    }
}
