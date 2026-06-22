import java.io.Serializable;
import javax.crypto.SealedObject;

public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String operacion; 
    private SealedObject payloadSeguro; // El contenido viaja protegido con AES
    private int relojLamport; 
    private String idOrigen; 

    public Mensaje(String operacion, Serializable payload, int relojLamport, String idOrigen) {
        this.operacion = operacion;
        this.relojLamport = relojLamport;
        this.idOrigen = idOrigen;
        try {
            this.payloadSeguro = Seguridad.encriptar(payload);
        } catch (Exception e) {
            System.err.println("Error al cifrar el payload del mensaje.");
        }
    }

    public String getOperacion() { return operacion; }
    public int getRelojLamport() { return relojLamport; }
    public String getIdOrigen() { return idOrigen; }
    
    public Object getPayload() {
        try {
            return Seguridad.desencriptar(this.payloadSeguro);
        } catch (Exception e) {
            return null;
        }
    }
}