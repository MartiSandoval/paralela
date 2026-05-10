import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Pelicula {
    String titulo;
    ArrayList<String> director = new ArrayList<>();
    Integer año;
    ArrayList<String> generos = new ArrayList<>();
    Path path; // Archivo de la película

    public Pelicula(String titulo, ArrayList<String> director, Integer año, ArrayList<String> generos) {
        this.titulo = titulo;
        this.director = director;
        this.año = año;
        this.generos = generos;
        try {
            String s = Main.class.getResourceAsStream("/peliculas/" + titulo + ".mp4").toString();
            System.out.println(s);
            this.path = Paths.get(s);
        } catch (Exception e) {
            System.out.println("Recurso no encontrado para la película: " + titulo);
            e.printStackTrace();
        }
    }
}