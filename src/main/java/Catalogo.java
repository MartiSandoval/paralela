import java.util.ArrayList;
import java.util.List;
public class Catalogo {
    private List<Pelicula> peliculas;

    public Catalogo() {
        peliculas = new ArrayList<>();
        peliculas.add(new Pelicula(1, "El Padrino", "Crimen"));
        peliculas.add(new Pelicula(2, "Forrest Gump", "Drama"));
    }

    public synchronized List<Pelicula> getPeliculas() {
        return new ArrayList<>(peliculas);
    }
}
