import java.io.Serializable;

public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String operacion;     // Ej: "SOLICITAR_CATALOGO", "VER_DETALLE"
    private Object payload;       // El contenido (Catálogo, Película, o null)
    private int relojLamport;     // La marca de tiempo lógico
    private String idOrigen;      // Quién emite el mensaje (Ej: "Cliente", "Nodo-1")

    public Mensaje(String operacion, Object payload, int relojLamport, String idOrigen) {
        this.operacion = operacion;
        this.payload = payload;
        this.relojLamport = relojLamport;
        this.idOrigen = idOrigen;
    }

    public String getOperacion() { return operacion; }
    public Object getPayload() { return payload; }
    public int getRelojLamport() { return relojLamport; }
    public String getIdOrigen() { return idOrigen; }
}