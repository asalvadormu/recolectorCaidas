package com.samuan.recolectorcaidas;

/**
 *
 * Clase para guardar datos de cada muestra.
 * Almacena el momento en que se capturan los datos y
 * el módulo del vector aceleración.
 *
 * Created by SAMUAN on 01/04/2015.
 */
public class Muestra {

    private long tiempo;
    private double aceleracion;

    public Muestra(long tiempo, double aceleracion) {
        this.tiempo = tiempo;
        this.aceleracion = aceleracion;
    }

    public long getTiempo() {
        return tiempo;
    }

    public void setTiempo(long tiempo) {
        this.tiempo = tiempo;
    }

    public double getAceleracion() {
        return aceleracion;
    }

    public void setAceleracion(double aceleracion) {
        this.aceleracion = aceleracion;
    }
}
