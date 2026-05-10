import java.nio.file.Paths;
import java.util.ArrayList;

public class Pelicula {
    String titulo;
    ArrayList<String> director = new ArrayList<>();
    Integer año;
    ArrayList<String> generos = new ArrayList<>();
    String path; // Path de la película

    public boolean getPelicula(String titulo, ArrayList<String> director, Integer año, ArrayList<String> generos) {
        try {
            this.path = Paths.get(Main.class.getResource("/peliculas/" + titulo.replace(" ", "-") + ".mp4").toURI()).toString();
            
            if(this.path == null) {
                throw new Exception("Archivo no encontrado para la película: " + titulo);
            }
        } catch (Exception e) {
            System.err.println("Archivo no encontrado para la película: " + titulo);
            e.printStackTrace();
            return false;
        }
        this.titulo = titulo;
        this.director = director;
        this.año = año;
        this.generos = generos;
        return true;
    }
}