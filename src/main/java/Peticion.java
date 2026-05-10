public class Peticion{
    private String accion; // ej: "LOGIN", "OBTENER_CATALOGO", "PEDIR_PELICULA"
    private Object objeto; // Puede ser un String, un objeto Pelicula, o una Lista

    public Peticion(String accion, Object objeto) {
        this.accion = accion;
        this.objeto = objeto;
    }

    public String getAccion() { return accion; }
    public Object getObjeto() { return objeto; }
}
