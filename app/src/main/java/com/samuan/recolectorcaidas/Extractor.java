package com.samuan.recolectorcaidas;

import android.util.Log;

/**
 *
 */
public class Extractor {

    private Muestra[] valores;

    private long peaktime;
    private long peaktimemas;
    private int marcadorPeak=0;
    private int marcadorPeakMas;

    private int marcadorIE=0; //posición en el vector del tiempo de fin de impacto valor_IE
    private long valor_IE =0;

    private int marcadorIS=0; //posición en el vector del tiempo de inicio de impacto valor_IS
    private long valor_IS =0;

    private double valor_AAMV;
    private double valor_IDI;
    private double valor_MPI;
    private double valor_MVI;
    private double valor_PDI;
    private double valor_ARI;
    private double valor_FFI;
    private int valor_SCI;

    private static String TAG="RedNeuronal";
    private double[] caracteristicas;

    /**
     * Calcula los 8 valores necesarios para el extractor
     *
     * @param peaktime tiempo de pico seguido de 2500 ms sin picos.
     * @param valores array con todos los valores capturados de aceleración
     */
    Extractor(long peaktime, Muestra[] valores){
       // Log.i("extractor", "Entrando en extractor");

        this.peaktime=peaktime;
        this.valores=valores;

        for(int i=0;i<valores.length;i++) {
            long eltiempo = valores[i].getTiempo();
            if (eltiempo >= peaktime) {
                marcadorPeak = i;
                break;
            }
        }

        //buscar indice de tiempo de pico mas 1 segundo
        peaktimemas=peaktime+1000000000; //nanosegundos
        marcadorPeakMas= valores.length-1;
        for(int i=marcadorPeak;i<valores.length;i++){
            if(valores[i].getTiempo()>peaktimemas){
                marcadorPeakMas=i;
                break;
            }
        }

        Log.i("EXTRACTOR","EXTRACTOR PEAK "+marcadorPeak+" "+peaktime );
        Log.i("EXTRACTOR","EXTRACTOR PEAK+1 "+marcadorPeakMas+" "+peaktimemas );
        //cálculo valores iniciales.
        calcularIE();
        calcularIS();

        Log.i("extractor", "Calculo Caracteristicas");
        //a partir de aqui son los cálculos de los ocho valores.
        valor_AAMV=calcularAAMV2(); //modificado para calcular en 1000 ms centrados en IS IE
        valor_IDI=calcularIDI();
        valor_MPI=calcularMPI();
        valor_MVI=calcularMVI();
        valor_PDI=calcularPDI();
        valor_ARI=calcularARI();
        valor_FFI=calcularFFI();
        valor_SCI=calcularSCI();

        FileOperation.fileLogWrite(TAG,"CARACTERISTICAS");
        FileOperation.fileLogWrite(TAG,"valor pico "+valores[marcadorPeak].getAceleracion());
        FileOperation.fileLogWrite(TAG,"valor peaktime "+peaktime);
        FileOperation.fileLogWrite(TAG,"valor peaktime +1 "+peaktimemas);
        FileOperation.fileLogWrite(TAG,"valor marcador peak "+marcadorPeak);
        FileOperation.fileLogWrite(TAG,"valor marcador peak mas "+marcadorPeakMas);

        String intervaloImpacto=" IS IE "+marcadorIS+" "+marcadorIE+" "+valor_IS+" "+valor_IE;
        FileOperation.fileLogWrite(TAG,"Intervalo Impacto: "+intervaloImpacto);

        String carcacte=""+valor_AAMV+", "+valor_IDI+", "+valor_MPI+", "+valor_MVI+", "+valor_PDI+", "+valor_ARI+", "+valor_FFI+", "+valor_SCI;
        FileOperation.fileLogWrite(TAG,"Carac: "+carcacte);

        FileOperation.fileLogWrite(TAG,"*****************************************************");

        caracteristicas=new double[8];
        caracteristicas[0]= valor_AAMV;
        caracteristicas[1]= valor_IDI;
        caracteristicas[2]= valor_MPI;
        caracteristicas[3]= valor_MVI;
        caracteristicas[4]= valor_PDI;
        caracteristicas[5]= valor_ARI;
        caracteristicas[6]= valor_FFI;
        caracteristicas[7]= valor_SCI;
    }


    public double[] getCaracteristicas() {
        return caracteristicas;
    }

    /**
     * Calcula el tiempo de fin de impacto. Última aceleracion mayor a 1.5 g dentro del intervalo [peaktime, peaktime + 1000]
     * @return
     */
    private void calcularIE(){
        for(int j=marcadorPeakMas; j>=marcadorPeak; j--){
            if(valores[j].getAceleracion()>1.5) {
                valor_IE =valores[j].getTiempo();
                marcadorIE=j;
                break;
            }
        }
        Log.i("EXTRACTOR","EXTRACTOR IE "+marcadorIE+" "+valor_IE );
    }

    /**
     * Calcula el tiempo de Inicio del impacto. Tiempo de primera aceleracion > 1.5 precedida de aceleración < 0.8 en
     * intervalo [ valor_IE-1200, Peaktime ]
     * @return
     */
    private void calcularIS() {
        long tiempoIEmenos = valor_IE - 1200000000;
        int marcadorTiempoIEMenos = 0;
        for (int i = 0; i < valores.length; i++) {
            long eltiempo = valores[i].getTiempo();
            if (eltiempo >= tiempoIEmenos) {
                marcadorTiempoIEMenos = i-1;
                break;
            }
        }
        //buscar un valor <0.8 y despues un valor > 1.5

        //si no aparece < 0.8 o despues >1.5 entonces peaktime.
        boolean elmenor=false;
        boolean elmayor=false;
        for(int j=marcadorTiempoIEMenos;j<=marcadorPeak;j++){

            if(!elmenor){
                if(valores[j].getAceleracion()<0.8){
                    elmenor=true;
                }
            }
            if(elmenor && !elmayor){
                if(valores[j].getAceleracion()>1.5){
                    elmayor=true;
                    marcadorIS=j;
                    valor_IS =valores[j].getTiempo();
                }
            }
        }
        if(!elmenor || !elmayor) {
            marcadorIS=marcadorPeak;
            valor_IS =peaktime;
        }

        Log.i("EXTRACTOR","EXTRACTOR IS "+marcadorIS+" "+valor_IS );
    }

    /**
     * Calcula el promedio absoluto de aceleración
     * En el intervalo [valor_IS,valor_IE]
     *
     */
    private double calcularAAMV(){
        double difTotal=0;
        if(marcadorIS>0 && marcadorIE<=valores.length) {
            for(int i=marcadorIS;i<=marcadorIE;i++){
                double dif= Math.abs(valores[i].getAceleracion() - valores[i + 1].getAceleracion());
                difTotal=difTotal+dif;
            }
            difTotal=difTotal/(marcadorIE-marcadorIS+1);
            Log.i("Acelerometro", "difTotal: " + difTotal);
        }
        Log.i("EXTRACTOR", "EXTRACTOR AAMV " +difTotal);

        return difTotal;
    }


    private double calcularAAMV2(){
        double valor=0;
        int marcaInferior=0;
        int marcaSuperior=valores.length;

        int marcaMedia=marcadorIS;
        Log.i("EXTRACTOR","EXTRACTOR AAMV 2 "+(marcadorIE-marcadorIS)/2);
        marcaMedia=marcadorIS+ (int) ((marcadorIE-marcadorIS)/2);
        Log.i("EXTRACTOR","EXTRACTOR marcaMedia"+marcaMedia);

        //buscar marca de tiempo 500 milisegundos antes
        for(int i=marcaMedia;i>0;i--){
            double difTiempo=valores[marcaMedia].getTiempo()-valores[i].getTiempo();
            if( difTiempo>500000000 ){
                marcaInferior = i;
                break;
            }
        }
        Log.i("EXTRACTOR","EXTRACTOR marcaInferior "+marcaInferior);
        //buscar marca de tiempo 500 milisegundos despues
        for(int i=marcaMedia;i<valores.length;i++){
            double difTiempo=valores[i].getTiempo()-valores[marcaMedia].getTiempo();
            if(difTiempo>500000000){
                marcaSuperior=i;
                break;
            }
        }
        Log.i("EXTRACTOR","EXTRACTOR marcaInferior "+marcaSuperior);

        //calcular el valor
        for(int i=marcaInferior;i<marcaSuperior;i++){
            double dif= Math.abs(valores[i].getAceleracion() - valores[i + 1].getAceleracion());
            valor=valor+dif;
        }
        valor=valor/(marcaSuperior-marcaInferior);
        Log.i("Acelerometro", "difTotal: " + valor);


        return valor;
    }

    /**
     * Calcula el indice de duración de impacto. IDI
     * Diferencia entre el tiempo valor_IE y el tiempo valor_IS
     */
    private double calcularIDI(){
        long tiempoFinal=valores[marcadorIE].getTiempo();
        long tiempoInicio=valores[marcadorIS].getTiempo();
        long tiempoAdevolver=(tiempoFinal-tiempoInicio)/1000000;
        Log.i("EXTRACTOR","EXTRACTOR IDI "+tiempoAdevolver );
        return tiempoAdevolver; //milisegundos.
    }

    /**
     * Calcula el valor del indice de valor máximo de aceleración. MPI
     * En el intervalo [valor_IS,valor_IE]
     * @return el valor de MPI en g.
     */
    private double calcularMPI(){
        double maxAceleracion =0;
        if(marcadorIS>0 && marcadorIE<=valores.length) {
            for(int i=marcadorIS;i<=marcadorIE;i++){
                if(maxAceleracion< valores[i].getAceleracion() ) {
                    maxAceleracion = valores[i].getAceleracion();
                }
            }
        }

        Log.i("EXTRACTOR","EXTRACTOR MPI "+maxAceleracion );
        return maxAceleracion;
    }

    /**
     * Calcula el índice de valle mínimo. MVI.
     * Es el valor mínimo de aceleración en el intervalo [valor_IS-500,valor_IE]
     * Útil para distinguir choques de caidas.
     *
     * @return el valor mínimo de aceleración.
     */
    private double calcularMVI(){
        //calcular posición valor_IS-500 000 000
        long tiempoISmenos = valor_IS - 500000000;
        int marcadorTiempoISMenos = 0;
        for (int i = 0; i < valores.length; i++) {
            long eltiempo = valores[i].getTiempo();
            if (eltiempo >= tiempoISmenos) { //en el peor caso marcará el valor i=0;
                marcadorTiempoISMenos = i;
                break;
            }
        }
        //calcular el MVI.
        double minAceleracion =1; //pongo 1 g. Una caida debe bajar de 1g.
        if(marcadorTiempoISMenos>0 && marcadorIE<=valores.length) {
            for(int i=marcadorTiempoISMenos;i<=marcadorIE;i++){
                if(minAceleracion> valores[i].getAceleracion() ) {
                    minAceleracion = valores[i].getAceleracion();
                }
            }
        }
        Log.i("EXTRACTOR","EXTRACTOR MVI "+minAceleracion );
        return minAceleracion;
    }

    /**
     * Calcula el índice de duración de pico. PDI.
     * Diferencia entre el PS comienzo de pico y PE fin de pico.
     * PS es el tiempo último muestreo <1.8g antes del pico
     * PE es el tiempo del primer muestreo <1.8g despues del pico.
     *
     * @return el valor de PDI en segundos.
     */
    private double calcularPDI(){
        int marcaPS=0;
        int marcaPE=valores.length-1;
        for(int i=marcadorPeak;i>=0;i--){
           if( valores[i].getAceleracion() <1.8){
               marcaPS=i;
               break;
           }
        }
        for(int i=marcadorPeak;i<valores.length;i++){
            if(valores[i].getAceleracion()<1.8){
                marcaPE=i;
                break;
            }
        }
        double valor= valores[marcaPE].getTiempo()-valores[marcaPS].getTiempo();
        valor=valor/1000000; //tiempo se necesita en milisegundos, no en nanosegundos.
        Log.i("EXTRACTOR","EXTRACTOR PDI  "+ valor+" | PS "+marcaPS+" "+valores[marcaPS].getTiempo()+ " PE "+marcaPE+" "+valores[marcaPE].getTiempo());
        return valor;
    }

    /**
     * Calcula el indice de ratio de actividad. ARI.
     * Mide el nivel de actividad en 700ms centrados en [valor_IS,valor_IE]
     * Ratio entre número de muestras no en [0.8g,1.3g] y total muestras en el intervalo.
     * Cuanto más alto mayor actividad.
     *
     * @return El valor de ARI.
     */
    private double calcularARI(){
        double valor=0;
        int marcaInferior=0;
        int marcaSuperior=valores.length;

        int marcaMedia=marcadorIS;
        System.out.println("EXTRACTOR ARI "+(marcadorIE-marcadorIS)/2);
        marcaMedia=marcadorIS+ (int) ((marcadorIE-marcadorIS)/2);
        System.out.println("EXTRACTOR marcaMedia"+marcaMedia);

        //buscar marca de tiempo 350  milisegundos antes
        for(int i=marcaMedia;i>0;i--){
            double difTiempo=valores[marcaMedia].getTiempo()-valores[i].getTiempo();
            if( difTiempo>350000000 ){
                marcaInferior = i;
                break;
            }
        }
        System.out.println("EXTRACTOR marcaInferior "+marcaInferior);
        //buscar marca de tiempo 350 milisegundos despues
        for(int i=marcaMedia;i<valores.length;i++){
            double difTiempo=valores[i].getTiempo()-valores[marcaMedia].getTiempo();
            if(difTiempo>350000000){
                marcaSuperior=i;
                break;
            }
        }

        //calcular el ari.
        double totalmuestras=marcaSuperior-marcaInferior;
        double muestrasEnIntervalo=0;
        for(int i=marcaInferior;i<=marcaSuperior;i++){
            double ace=valores[i].getAceleracion();
            if( ace<0.8 || ace>1.5 ){
                muestrasEnIntervalo++;
            }
        }
        valor=muestrasEnIntervalo/totalmuestras;
        return valor;
    }


    /**
     * Calcula el indice de caida libre. FFI.
     * Busca muestra <0.8g 200ms antes del pico. El tiempo encontrado es el fin del intervalo, si no 200ms.
     * Inicio del intervalo 200ms antes del fin.
     *
     * Se calcula como media de valores de aceleración
     * saltos 0.1g caidas minimo 0.6, media 1.1g
     *
     * @return el valor de FFI
     */
    private double calcularFFI(){
        int marcaInicio=0;
        int marcaFin=marcadorPeak;

        //averiguar fin de intervalo
        for(int i=marcadorPeak-1;i>=0;i--){
           double difTiempo=peaktime-valores[i].getTiempo();
           if(valores[i].getAceleracion() < 0.8   ){
               marcaFin=i;
               break;
           }else if( difTiempo>200000000  ){
               if(i==marcadorPeak-1){
                   marcaFin=i;
               }else{
                   marcaFin=i-1;
               }
               break;
           }
        }
        //averiguar inicio intervalo.
        double tiempoInicio=valores[marcaFin].getTiempo()-200000000;
        for(int i=marcaFin-1;i>=0;i--){
            double difTiempo=tiempoInicio-valores[i].getTiempo();
            if(difTiempo>200000000){
                marcaInicio=i;
                break;
            }
        }
        //calcular media.
        double contador=0;
        for(int i=marcaInicio;i<=marcaFin;i++){
            contador=contador+valores[i].getAceleracion();
        }
        double valor=contador/(marcaFin-marcaInicio+1);
        Log.i("EXTRACTOR","EXTRACTOR FFI "+valor );
        return valor;
     }


    /**
     * Calcula el indice de contador de pasos. SCI
     * Contar valles 2200ms antes del PT.
     * Valle aceleración < 1g durante al menos 80 ms
     * seguido de pico >1.6g dentro de 200 ms.
     * Valles consecutivos separados más de 200ms.
     *
     * @return el valor de SCI
     */
    private int calcularSCI(){
        //calcular marca en tiempo anterior a 2200ms
        int marcaInicio=0;
        double tiempoInicio=peaktime-2200000000d;
        for(int i=marcadorPeak-1;i<=0;i--){
            if( valores[i].getTiempo() <tiempoInicio){
                marcaInicio=i;
                break;
            }
        }

        String modo="inicio";
        double tiempoInicioValle=0;
        double tiempoFinValle=0;
        double tiempoPasoAnterior=0;
        int marcaFinValle=0;

        int contadorPasos=0;

        //calculo pasos
        int marcaActual=marcaInicio; //indica el indice actual que se esta comprobando.
        while(marcaActual<marcadorPeak){
            if( modo.equals("inicio")){

                if(valores[marcaActual].getAceleracion()<1){
                    tiempoInicioValle=valores[marcaActual].getTiempo();
                    modo="valle";
                }

                marcaActual++;

            } else if( modo.equals("valle")){
                if(valores[marcaActual].getAceleracion()>=1){
                    tiempoFinValle=valores[marcaActual-1].getTiempo();
                    double dif=tiempoFinValle-tiempoInicioValle;
                    if(dif>80000000){
                         marcaFinValle=marcaActual-1;
                        if(contadorPasos>0     ){
                            if( (tiempoFinValle-tiempoPasoAnterior)>200000000 ){
                                modo="pico";
                            }else{  //el valle está demasiado cerca, hay que descartar al primero y empezar desde este.
                                modo="inicio";

                            }
                        }else{
                            modo="pico";
                        }

                    }else{
                        modo="inicio";
                    }
                }
                marcaActual++;

            }else if(modo.equals("pico")){
                double difLimite= valores[marcaActual].getTiempo()- tiempoFinValle;
                if(difLimite<200000000){
                    if(valores[marcaActual].getAceleracion()>1.6) {
                        contadorPasos++;
                        tiempoPasoAnterior=valores[marcaFinValle].getTiempo();
                        modo="inicio";

                    } else{
                        //no hago nada
                    }
                }else{
                    modo="inicio";
                    marcaActual=marcaFinValle;
                }
                marcaActual++;
            }
        }
        Log.i("EXTRACTOR","EXTRACTOR SCI "+contadorPasos );
        return contadorPasos;
    }

}






