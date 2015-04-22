package com.samuan.recolectorcaidas;

import android.os.Environment;
import android.text.format.DateFormat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by SAMUAN on 13/04/2015.
 */
public class FileOperation {

    private static String archivoLog="redNeuronal.log";

    public static void fileLogInitialize() {
        File sdcard = Environment.getExternalStorageDirectory();
        File logFile = new File(sdcard,archivoLog);

        if (logFile.exists()) {
            fileLogErase();
        }
    }

    /**
     * Borrado del fichero de log
     */
    public static void fileLogErase() {

        File sdcard = Environment.getExternalStorageDirectory();
        File logFile = new File(sdcard,archivoLog);

        try {
            logFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Un mensaje de debug tienen la opción de ser escritos a un fichero LOG
     * esta opción de LOG se define en Constants (LOG_TO_FILE = false;)
     *
     * @param tag La clase o módulo donde se lanza el mensaje
     * @param msg El mensaje generado
     */
    public static void fileLogWrite(String tag, String msg) {
        {
            //Timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
            String currentDateandTime = sdf.format(new Date());
            String text =currentDateandTime+" "+ tag + "--> " + msg;

            File sdcard = Environment.getExternalStorageDirectory();
            File logFile = new File(sdcard,archivoLog);

            //////////// En el caso que no quisieramos borrar el fichero ///////
            if (!logFile.exists()) {
                try {
                    logFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                //BufferedWriter for performance, true to set append to file flag
                BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
                buf.append(text);
                buf.newLine();
                buf.flush(); //Creo que esto es necesario
                buf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
