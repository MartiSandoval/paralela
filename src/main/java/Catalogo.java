import java.util.ArrayList;

public class Catalogo {
    ArrayList<Pelicula> peliculas = new ArrayList<>();
    ArrayList<String> recomendaciones = new ArrayList<>();

    public Catalogo(ArrayList<String> lista_peliculas, int num_movies) {
        for(int i = 0; i < num_movies; i++) {
            String[] datos = lista_peliculas.get(i).split(";"); 
            
            String titulo = datos[0];
            
            ArrayList<String> director = new ArrayList<>();
            for(String g : datos[1].split(",")) { director.add(g); }
            
            Integer año = Integer.parseInt(datos[2]);
            
            ArrayList<String> generos = new ArrayList<>();
            for(String g : datos[3].split(",")) { generos.add(g); }
            
            Pelicula p = new Pelicula();
            if(p.getPelicula(titulo, director, año, generos)) {
                peliculas.add(p);
            }
        }
    }

    public synchronized Pelicula getPeliculaPorTitulo(String titulo) {
        for (Pelicula p : peliculas) {
            if (p.getTitulo().equalsIgnoreCase(titulo)) {
                return p;
            }
        }
        return null;
    }

    public ArrayList<Pelicula> getPeliculas() { return peliculas; }
}