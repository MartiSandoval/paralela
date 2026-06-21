import javax.crypto.Cipher;
// import javax.crypto.KeyGenerator;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import java.io.Serializable;
import javax.crypto.spec.SecretKeySpec;

public class Seguridad {
    // Llave simétrica estática para que todos los nodos puedan entenderse
    private static final byte[] LLAVE_COMPARTIDA = "NetflixDistribu1".getBytes(); 
    private static SecretKey claveSecreta;
    static {
        // Inicializamos el motor usando nuestra llave estática en lugar de una aleatoria
        claveSecreta = new SecretKeySpec(LLAVE_COMPARTIDA, "AES");
    }

    public static SealedObject encriptar(Serializable objeto) throws Exception {
        if (objeto == null) return null;
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, claveSecreta);
        return new SealedObject(objeto, cipher);
    }

    public static Object desencriptar(SealedObject objetoSellado) throws Exception {
        if (objetoSellado == null) return null;
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, claveSecreta);
        return objetoSellado.getObject(cipher);
    }
}