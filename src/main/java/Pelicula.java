import java.io.Serializable;

public class Pelicula implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String titulo;
    private String genero;
    
    public Pelicula(int id, String titulo, String genero) {
        this.id = id;
        this.titulo = titulo;
        this.genero = genero;
    }

    public int getId() {
        return id;
    }
    public String getTitulo() {
        return titulo;
    }
    public String getGenero() {
        return genero;
    }
    public int setId(int id) {
        return this.id = id;
    }
    public String setTitulo(String titulo) {
        return this.titulo = titulo;
    }
    public String setGenero(String genero) {
        return this.genero = genero;
    }
    public String toString() {
        return "ID: " + id + ", Título: " + titulo + ", Género: " + genero;
    }
}