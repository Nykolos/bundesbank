package bundesbank.file;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

public class FileReader {

    private static ArrayList<String> readFile(String fileName) {
        ArrayList<String> lines = new ArrayList<>();

        ClassLoader classLoader = FileReader.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException | NullPointerException e) {
            System.err.println("Fehler beim Lesen der Datei: " + e.getMessage());
        }

        return lines;
    }

    public static Task parseFile(String fileName) {
        ArrayList<String> lines = readFile(fileName);
        String[] l;

        lines.forEach(System.out::println);

        l = lines.removeFirst().split(" ");
        int coins = Integer.parseInt(l[0]);
        int banks = Integer.parseInt(l[1]);
        int days = Integer.parseInt(l[2]);

        l = lines.removeFirst().split(" ");
        int[] coinValues = new int[coins];
        for (int i = 0; i < coins; i++) {
            coinValues[i] = Integer.parseInt(l[i]);
        }

        ArrayList<Bank> bankList = new ArrayList<>();
        for (int i = 0; i < banks; i++) {
            int bankCoins = 0, bankDays = 0, transactionCount = 0;

            l = lines.removeFirst().split(" ");
            bankCoins = Integer.parseInt(l[0]);
            bankDays = Integer.parseInt(l[1]);
            transactionCount = Integer.parseInt(l[2]);

            int[] bankCoinIDs = new int[bankCoins];

            l = lines.removeFirst().split(" ");
            for (int j = 0; j < bankCoins; j++) {
                bankCoinIDs[j] = Integer.parseInt(l[j]);
            }

            bankList.add(new Bank(bankCoins, bankDays, transactionCount, bankCoinIDs));
        }

        return new Task(coins, banks, days, coinValues, bankList);
    }
}