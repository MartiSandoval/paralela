import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        ArrayList<String> m = new ArrayList<>();;
        try (InputStream is = Main.class.getResourceAsStream("/peliculas/lista_peliculas.txt");
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while((line = br.readLine()) != null) { m.add(line); }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al cargar archivo de peliculas.");
        }

        Catalogo c = new Catalogo(m, m.size());

        ArrayList<Pelicula> p = c.peliculas;
        for(Pelicula pel : p) {
            System.out.println("Título: " + pel.titulo);
            System.out.println("Director: " + String.join(", ", pel.director));
            System.out.println("Año: " + pel.año);
            System.out.println("Géneros: " + String.join(", ", pel.generos));
            System.out.println("Path: " + pel.path);
            System.out.println();
        }

    }
}
