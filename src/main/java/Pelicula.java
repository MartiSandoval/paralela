import java.util.ArrayList;

public class Pelicula{
    private String nombre;
    private String director;
    private Integer year;
    private ArrayList<String> generos = new ArrayList<>();
    
    public Pelicula(String nombre, String director, Integer year) {
        this.nombre = nombre;
        this.director = director;
        this.year = year;
    }
    public String getNombre() { return nombre; }
    public String getDirector() { return director; }
    public Integer getYear() { return year; }
    
    @Override
    public String toString() {
        return nombre + " (" + year + ") - Dir: " + director;
    }
    
}
