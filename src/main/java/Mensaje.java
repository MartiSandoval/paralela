/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author marti
 */
import java.io.Serializable;
import javax.crypto.SealedObject;

public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String operacion; // Ej: "SOLICITAR_CATALOGO", "PLAY", "HEARTBEAT"
    private SealedObject payloadSeguro; // El ArrayList o String cifrado
    private int relojLamport; // Requisito Unidad 5
    private int puertoOrigen; // Para saber qué nodo de la topología lo envía
}
