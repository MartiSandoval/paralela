/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author marti
 */
import java.io.Serializable;

public class Peticion implements Serializable{
    private static final long serialVersionUID = 1L;
    private String accion; // ej: "LOGIN", "OBTENER_CATALOGO", "PEDIR_PELICULA"
    private Object objeto; // Puede ser un String, un objeto Pelicula, o una Lista

    public Peticion(String accion, Object objeto) {
        this.accion = accion;
        this.objeto = objeto;
    }

    public String getAccion() { return accion; }
    public Object getObjeto() { return objeto; }
}
