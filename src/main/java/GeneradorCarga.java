import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class GeneradorCarga {
    private static final int HILOS_CONCURRENTES = 50;
    private static final int TIEMPO_PRUEBA_SEGUNDOS = 60;
    
    private static final int[] PUERTOS_CATALOGO = {5001, 5002, 5003};

    private static AtomicInteger peticionesExitosas = new AtomicInteger(0);
    private static AtomicInteger peticionesFallidas = new AtomicInteger(0);
    private static List<Long> latencias = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        System.out.println("=== INICIANDO PRUEBA DE ESTRÉS ===");
        System.out.println("Lanzando " + HILOS_CONCURRENTES + " clientes concurrentes por " + TIEMPO_PRUEBA_SEGUNDOS + " segundos...");
        
        ExecutorService pool = Executors.newFixedThreadPool(HILOS_CONCURRENTES);
        long tiempoInicioPrueba = System.currentTimeMillis();

        for (int i = 0; i < HILOS_CONCURRENTES; i++) {
            pool.execute(new ClienteRobot(tiempoInicioPrueba, i));
        }

        pool.shutdown();
        try { pool.awaitTermination(TIEMPO_PRUEBA_SEGUNDOS + 5, TimeUnit.SECONDS); } 
        catch (InterruptedException e) { e.printStackTrace(); }

        imprimirMetricasFinales(tiempoInicioPrueba);
    }

    static class ClienteRobot implements Runnable {
        private long inicioPrueba;
        private String idRobot;

        public ClienteRobot(long inicioPrueba, int id) {
            this.inicioPrueba = inicioPrueba;
            this.idRobot = "Robot-" + id;
        }

        @Override
        public void run() {
            int relojCliente = 0;
            
            while ((System.currentTimeMillis() - inicioPrueba) < (TIEMPO_PRUEBA_SEGUNDOS * 1000)) {
                long inicioPeticion = System.currentTimeMillis();
                boolean exito = false;
                relojCliente++;

                for (int puertoDestino : PUERTOS_CATALOGO) {
                    try (Socket s = new Socket("127.0.0.1", puertoDestino);
                         ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                         ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {

                        // Envíamos la petición encriptada con reloj Lamport
                        Mensaje msjEnvio = new Mensaje("SOLICITAR_CATALOGO", null, relojCliente, idRobot);
                        out.writeObject(msjEnvio);
                        out.flush();

                        Mensaje respuesta = (Mensaje) in.readObject();
                        relojCliente = Math.max(relojCliente, respuesta.getRelojLamport()) + 1;
                        
                        exito = true;
                        break; 
                    } catch (Exception e) {
                        // Tolerancia a fallos: salta al siguiente nodo si el puerto cae
                    }
                }

                long finPeticion = System.currentTimeMillis();
                if (exito) {
                    peticionesExitosas.incrementAndGet();
                    latencias.add(finPeticion - inicioPeticion);
                } else {
                    peticionesFallidas.incrementAndGet();
                }

                try { Thread.sleep(50); } catch (InterruptedException e) {}
            }
        }
    }

    private static void imprimirMetricasFinales(long tiempoInicio) {
        long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
        int exitosas = peticionesExitosas.get();
        int fallidas = peticionesFallidas.get();

        System.out.println("\n==============================================");
        System.out.println("      RESULTADOS DE LA PRUEBA DE CARGA");
        System.out.println("==============================================");
        System.out.println("Tiempo total      : " + (tiempoTotal / 1000.0) + " seg");
        System.out.println("Peticiones OK     : " + exitosas);
        System.out.println("Peticiones Error  : " + fallidas);

        if (exitosas > 0) {
            double throughput = exitosas / (tiempoTotal / 1000.0);
            System.out.printf("Throughput        : %.2f peticiones/seg\n", throughput);

            long suma = 0;
            for (long lat : latencias) suma += lat;
            System.out.println("Latencia Promedio : " + (suma / exitosas) + " ms");

            Collections.sort(latencias);
            int indiceP95 = (int) Math.ceil((95.0 / 100.0) * latencias.size()) - 1;
            System.out.println("Latencia p95      : " + latencias.get(Math.max(0, indiceP95)) + " ms");
        }
        System.out.println("==============================================");
    }
}