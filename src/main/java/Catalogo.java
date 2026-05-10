import java.util.ArrayList;
import java.util.List;

public class Catalogo {
    private ArrayList<Pelicula> peliculas = new ArrayList<>();
    // private ArrayList<String> recomendaciones = new ArrayList<>();

    public Catalogo() {
        ArrayList<String> datosSimulados = new ArrayList<>();
        datosSimulados.add("Inception;Christopher Nolan;2010;Sci-Fi,Action");
        datosSimulados.add("The Matrix;Lana Wachowski,Lilly Wachowski;1999;Sci-Fi,Action");
        
        cargarDatos(datosSimulados, datosSimulados.size());
    }

    public Catalogo(ArrayList<String> lista_peliculas, int num_movies) {
        cargarDatos(lista_peliculas, num_movies);
    }

    private void cargarDatos(ArrayList<String> lista_peliculas, int num_movies) {
        for(int i = 0; i < num_movies; i++) {
            String[] datos = lista_peliculas.get(i).split(";"); 
            
            String titulo = datos[0];
            
            ArrayList<String> director = new ArrayList<>();
            for(String d : datos[1].split(",")) { director.add(d); }
            
            Integer año = Integer.parseInt(datos[2]);
            
            ArrayList<String> generos = new ArrayList<>();
            for(String g : datos[3].split(",")) { generos.add(g); }

            String path = (datos.length > 4) ? datos[4] : "Ruta no definida";
            
   
            Pelicula p = new Pelicula(titulo, director, año, generos, path);
            peliculas.add(p);
        }
    }
    
    public synchronized List<Pelicula> getPeliculas() {
        return new ArrayList<>(peliculas); 
    }
    
}