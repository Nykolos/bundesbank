package bundesbank.file;

import java.io.IOException;
import java.io.FileWriter;

public class FileSaver {
    FileWriter writer;

    public FileSaver(String fileName) {
        // Constructor
        try {
            writer = new FileWriter(fileName);
        } catch (IOException e) {
            System.out.println("Fehler beim Öffnen der Datei.");
        }
    }

    public void append(String msg) {
        try {
            writer.write(msg + "\n");
            writer.flush();
        } catch (IOException e) {
            System.out.println("Fehler beim Schreiben in die Datei.");
        }
    }

    public void appendIn(String msg) {
        try {
            writer.write(msg + " ");
            writer.flush();
        } catch (IOException e) {
            System.out.println("Fehler beim Schreiben in die Datei.");
        }
    }

}