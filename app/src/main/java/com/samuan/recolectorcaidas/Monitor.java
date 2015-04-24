package com.samuan.recolectorcaidas;

import android.content.res.Resources;
import android.hardware.SensorEvent;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

/**
 * Comprueba los datos del acelerometro.
 *
 * Created by SAMUAN on 13/04/2015.
 */
public class Monitor {

    private float gravedad=9.8066f;

    private LinkedList<Muestra> cola;
    private int tamaLista=500;

    private double umbralGravedad=2.1;

    private long pt=0; //peak time
    private long contadorTiempo=0;
    private String estado="muestreo";
    Muestra[] datos=null;

    private static String TAG="RedNeuronal";

    private long tiempoInicio;
    private long tiempoActual;
    private long tiempoPasado;

    public Monitor(Resources resources) {
        FileOperation.fileLogInitialize();
        FileOperation.fileLogWrite(TAG,"Inicio app: ");
        FileOperation.fileLogWrite(TAG,"Umbral gravedad: "+umbralGravedad);

        cola =new LinkedList<Muestra>();

        tiempoInicio=System.currentTimeMillis();
        tiempoPasado=System.currentTimeMillis();


    }

    /**
     * Gestiona los eventos del acelerometro. Si se cumplen las caracteristicas extrae caracteristicas
     *
     *
     * @param event
     */
    public void gestionar(SensorEvent event) {

        //apuntador de tiempo de prueba...
        tiempoActual=System.currentTimeMillis();
        if( ( tiempoActual - tiempoPasado) > 5000 ){
            tiempoPasado=tiempoActual;
            FileOperation.fileLogWrite(TAG,"segundos "+ (tiempoPasado-tiempoInicio)/1000);
        }

        float values[] = event.values;
        float x = values[0];
        float y = values[1];
        float z = values[2];
        float xg=x/gravedad; //Divido por gravedad para pasar unidades de m/s^2 a g
        float yg=y/gravedad;
        float zg=z/gravedad;

        long tiempo=event.timestamp;
        double modulo=calcularModulo(xg,yg,zg);

        cargarMuestra(new Muestra(tiempo,modulo));
      //  Log.i("MONITOR","gestionar "+modulo);
        if(estado.equals("muestreo")){ //se ha detectado un pico de gravedad
            if(modulo>umbralGravedad){
                iniciarPostpeak(modulo,tiempo);
            }
        }

        if(estado.equals("postpeak")){ //Ahora esperamos un tiempo de 2.5 segundos sin picos superiores al umbral.
            contadorTiempo=tiempo-pt;
            if(modulo>umbralGravedad) iniciarPostpeak(modulo,tiempo); //si se detecta un nuevo pico, comenzamos a contar el tiempo de nuevo.
            if(contadorTiempo>2500000000l){
                //generar array de valores.
                datos=new Muestra[cola.size()];
                cola.toArray(datos); //extraigo datos a analizar.
                //FileOperation.fileLogWrite(TAG, "Test de actividad ");
                iniciarActivityTest();
                Log.i("Acelerometro","iniciar activity test "+tiempo);
            }
        }

    }

    /**
     * Realizo el test de actividad. Si la actividad es baja se extraen caracteristicas y
     * se pasa a red neuronal.
     *
     * La respuesta de la red neuronal se para a archivo.
     */
    private void iniciarActivityTest(){
        //capturar datos de lista
        estado="activitytest";
        Log.i("Acelerometro","iniciar activity test");

        //calcular AAMV , media de las diferencias.
        long tiempoInicioCalculo=pt+1000000000; //se toma desde 1 sg a 2.5 sg despues del impacto
        int marcador=0;
        double difTotal=0;
        long tiempoFinalCalculo = pt + 2500000000l;
        int marcadorFin=datos.length-1;


        for(int i=0;i<datos.length;i++){
            //buscar el dato con tiempo > tiempoIniciocalculo
            if( datos[i].getTiempo()>tiempoInicioCalculo ){
                marcador=i;
                break;
            }
        }
        for(int i=marcador;i<datos.length;i++){
            if(datos[i].getTiempo()>tiempoFinalCalculo){
                marcadorFin=i;
                break;
            }
        }
        for(int j=marcador;j<marcadorFin;j++){
            double dif=Math.abs( datos[j].getAceleracion() - datos[j+1].getAceleracion() );
            difTotal=difTotal+dif;
        }
        //difTotal=difTotal/(datos.length-marcador); //divide entre mas datos --> valor mas pequeño
        difTotal=difTotal/(marcadorFin-marcador);
        Log.i(TAG,"Filtro AAMV: "+difTotal);

        FileOperation.fileLogWrite(TAG,"Filtro AAMV Test Actividad: "+difTotal);

        //si valor supera 0.05g entonces se descarta como caida
        //si es menor o igual se considera caida y se envian datos a clasificador
        if(difTotal>0.05){

        }else {
            //FileOperation.fileLogWrite(TAG,"Envío a Red Neuronal");

            Log.i(TAG,"tiempo de pico "+pt);
            Extractor extractor = new Extractor(pt, datos);
            //extractor.getCaracteristicas();

        }
        estado="muestreo";
    }

    /**
     * Cambia el estado a "postpeak".
     *
     * @param modulo
     * @param tiempo
     */
    private void iniciarPostpeak(double modulo,long tiempo){
        contadorTiempo=0;
        pt=tiempo;
        estado="postpeak";
        // System.out.println("iniciar post peak "+tiempo);
        //FileOperation.fileLogWrite(TAG,"Post peak | Modulo: "+modulo+" Tiempo: "+tiempo);
        Log.i(TAG,"Post peak | Modulo: "+modulo+" Tiempo: "+tiempo);
    }

    /**
     * Añade un objeto muestra a la cola.
     * Si la cola se llena elimina por la cabeza.
     *
     * @param muestra
     */
    private void cargarMuestra(Muestra muestra){
        cola.add(muestra);
        if(cola.size()>tamaLista) cola.poll();
    }

    /**
     * Calcula el módulo del vector aceleración dado por el acelerómetro
     * @param x
     * @param y
     * @param z
     * @return
     */
    private double calcularModulo(double x, double y, double z){
        return Math.sqrt(    Math.pow(x,2) + Math.pow(y,2)+ Math.pow(z,2)   );
    }

}
