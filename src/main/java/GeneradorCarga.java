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
    // Parámetros exigidos por la rúbrica
    private static final int HILOS_CONCURRENTES = 50;
    private static final int TIEMPO_PRUEBA_SEGUNDOS = 60;
    
    // Topología actualizada
    private static final int[] PUERTOS_CATALOGO = {5001, 5002, 5003};

    // Contadores seguros para concurrencia (Métricas)
    private static AtomicInteger peticionesExitosas = new AtomicInteger(0);
    private static AtomicInteger peticionesFallidas = new AtomicInteger(0);
    private static List<Long> latencias = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        System.out.println("=== INICIANDO PRUEBA DE ESTRÉS ===");
        System.out.println("Lanzando " + HILOS_CONCURRENTES + " clientes concurrentes por " + TIEMPO_PRUEBA_SEGUNDOS + " segundos...");
        
        ExecutorService pool = Executors.newFixedThreadPool(HILOS_CONCURRENTES);
        long tiempoInicioPrueba = System.currentTimeMillis();

        // Disparamos los 50 hilos al mismo tiempo
        for (int i = 0; i < HILOS_CONCURRENTES; i++) {
            pool.execute(new ClienteRobot(tiempoInicioPrueba));
        }

        // Apagamos la admisión de nuevos hilos y esperamos que terminen los 60 segundos
        pool.shutdown();
        try {
            pool.awaitTermination(TIEMPO_PRUEBA_SEGUNDOS + 5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        imprimirMetricasFinales(tiempoInicioPrueba);
    }

    // --- HILO AUTOMATIZADO (El Robot) ---
    static class ClienteRobot implements Runnable {
        private long inicioPrueba;

        public ClienteRobot(long inicioPrueba) {
            this.inicioPrueba = inicioPrueba;
        }

        @Override
        public void run() {
            int relojCliente = 0;
            
            // Bucle que se repite sin parar hasta que pasen los 60 segundos
            while ((System.currentTimeMillis() - inicioPrueba) < (TIEMPO_PRUEBA_SEGUNDOS * 1000)) {
                long inicioPeticion = System.currentTimeMillis();
                boolean exito = false;
                relojCliente++;

                // Tolerancia a fallos: Intenta conectar, si falla, salta al siguiente puerto
                for (int puertoDestino : PUERTOS_CATALOGO) {
                    try (Socket s = new Socket("127.0.0.1", puertoDestino);
                         ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                         ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {

                        // Pedimos el catálogo usando el protocolo seguro
                        Mensaje msjEnvio = new Mensaje("SOLICITAR_CATALOGO", null, relojCliente, 0);
                        out.writeObject(msjEnvio);
                        out.flush();

                        Mensaje respuesta = (Mensaje) in.readObject();
                        relojCliente = Math.max(relojCliente, respuesta.getRelojLamport()) + 1;
                        
                        exito = true;
                        break; // Si salió bien, salimos del for de puertos
                    } catch (Exception e) {
                        // Falla controlada, el robot intentará con el siguiente puerto
                    }
                }

                long finPeticion = System.currentTimeMillis();
                
                if (exito) {
                    peticionesExitosas.incrementAndGet();
                    latencias.add(finPeticion - inicioPeticion);
                } else {
                    peticionesFallidas.incrementAndGet();
                }

                // Pausa de 50ms para no agotar los puertos TCP del sistema operativo local
                try { Thread.sleep(50); } catch (InterruptedException e) {}
            }
        }
    }

    // --- CÁLCULO MATEMÁTICO (Rendimiento) ---
    private static void imprimirMetricasFinales(long tiempoInicio) {
        long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
        int exitosas = peticionesExitosas.get();
        int fallidas = peticionesFallidas.get();

        System.out.println("\n==============================================");
        System.out.println("      RESULTADOS DE LA PRUEBA DE CARGA");
        System.out.println("==============================================");
        System.out.println("Tiempo total de ejecución : " + (tiempoTotal / 1000.0) + " segundos");
        System.out.println("Peticiones Exitosas       : " + exitosas);
        System.out.println("Peticiones Fallidas       : " + fallidas);

        if (exitosas > 0) {
            double throughput = exitosas / (tiempoTotal / 1000.0);
            System.out.printf("Throughput (Rendimiento)  : %.2f peticiones/segundo\n", throughput);

            long sumaLatencias = 0;
            for (long lat : latencias) sumaLatencias += lat;
            long promedio = sumaLatencias / exitosas;
            System.out.println("Latencia Promedio         : " + promedio + " ms");

            Collections.sort(latencias);
            int indiceP95 = (int) Math.ceil((95.0 / 100.0) * latencias.size()) - 1;
            long p95 = latencias.get(Math.max(0, indiceP95));
            System.out.println("Latencia Percentil 95     : " + p95 + " ms");
        }
        System.out.println("==============================================");
    }
}