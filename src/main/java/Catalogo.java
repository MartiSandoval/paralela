import java.util.ArrayList;

public class Catalogo {
    ArrayList<Pelicula> peliculas = new ArrayList<>();
    ArrayList<String> recomendaciones = new ArrayList<>();

    public Catalogo(ArrayList<String> lista_peliculas, int num_movies) {
        for(int i = 0; i < num_movies; i++) {
            String[] datos = lista_peliculas.get(i).split(";"); // Suponiendo formato: nombre,director,año,genero1|genero2|...,archivo
            String titulo = datos[0];
            ArrayList<String> director = new ArrayList<>();
            for(String g : datos[1].split(",")) { director.add(g); }
            Integer año = Integer.parseInt(datos[2]);
            ArrayList<String> generos = new ArrayList<>();
            for(String g : datos[3].split(",")) { generos.add(g); }
            //System.out.println("Datos pelicula:" + datos[0] + " " + datos[1] + " " + datos[2] + " " + datos[3]);
            peliculas.add(new Pelicula(titulo, director, año, generos));
        }
    }
}
