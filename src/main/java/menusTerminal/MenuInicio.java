package menusTerminal;

public class MenuInicio {
    /*Simplemente muesta el menú de inicio y
    los submenús*/
    public void mostrarInicio() {
        System.out.println("\n===============Netflix===============\n");
        System.out.println("1. Ver recomendaciones");
        System.out.println("2. Mostrar catálogo"); 
        System.out.println("3. Salir del sistema");
        System.out.print("Seleccione una opción: ");
    }
    
    public void mostrarBusqueda() {
        System.out.println("\n===============Catálago de Películas===============\n");
        System.out.println("1. Filtrar peliculas");
        System.out.println("2. Buscar pelicula");
        System.out.println("3. Volver al menú inicial"); 
        System.out.print("Seleccione una opción: ");
    
}
}