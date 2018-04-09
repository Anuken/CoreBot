package io.anuke.corebot;

import java.io.*;
import java.util.Properties;

public class Prefs {
    private Properties prop = new Properties();
    private File file;

    public Prefs(File file){
        this.file = file;
        try {
            if(!file.exists()) file.createNewFile();
            prop = new Properties();
            prop.load(new FileInputStream(file));
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public String get(String property, String def){
        return prop.getProperty(property, def);
    }

    public int getInt(String property, int def){
        return Integer.parseInt(prop.getProperty(property, def+""));
    }

    public void put(String property, String value){
        prop.put(property, value);
        save();
    }

    public void save(){
        try {
            prop.store(new FileOutputStream(file), null);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }
}
