import java.io.Serializable;
import java.util.ArrayList;

public class Pelicula implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String titulo;
    private ArrayList<String> director;
    private Integer año;
    private ArrayList<String> generos;
    private String path;

    public Pelicula(String titulo, ArrayList<String> director, Integer año, ArrayList<String> generos, String path) {
        this.titulo = titulo;
        this.director = director;
        this.año = año;
        this.generos = generos;
        this.path = path;
    }

    public String getTitulo() { return titulo; }
    public ArrayList<String> getDirector() { return director; }
    public Integer getAño() { return año; }
    public ArrayList<String> getGeneros() { return generos; }
    public String getPath() { return path; }

    @Override
    public String toString() {
        return titulo + " (" + año + ") - " + String.join(", ", generos);
    }
}