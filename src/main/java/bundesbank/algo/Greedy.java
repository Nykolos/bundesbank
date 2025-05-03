package bundesbank.algo;

import bundesbank.file.Bank;
import bundesbank.file.FileSaver;
import bundesbank.file.Task;

import java.util.*;

public class Greedy {
    public static void solve(Task task, int days, FileSaver saver) {
        List<Bank> banks = task.bankList();
        int[] coinValues = task.coinValues();
        int B = banks.size();

        // 1) Pro Bank: IDs sortieren (absteigend nach Wert)
        int[][] sortedCoins = new int[B][];
        for (int j = 0; j < B; j++) {
            Bank b = banks.get(j);
            int[] raw = b.bankCoinIDs();
            Integer[] boxed = Arrays.stream(raw).boxed().toArray(Integer[]::new);
            Arrays.sort(boxed, (a, b2) -> Integer.compare(coinValues[b2], coinValues[a]));
            sortedCoins[j] = Arrays.stream(boxed).mapToInt(i -> i).toArray();
        }

        // 2) Für jede Bank einen statischen Score berechnen: Summe der Top-cap-Werte / Registrierungsdauer
        record Candidate(int idx, Bank bank, int[] coins, double score) {}
        List<Candidate> cand = new ArrayList<>(B);

        for (int j = 0; j < B; j++) {
            Bank b = banks.get(j);
            int regDays = b.bankDays();
            int maxSend = b.transactionCount() * Math.max(0, days - regDays);
            int cap = Math.min(maxSend, sortedCoins[j].length);
            long sum = 0;
            for (int k = 0; k < cap; k++) {
                sum += coinValues[sortedCoins[j][k]];
            }
            double score = regDays > 0 ? (double) sum / regDays : 0;
            cand.add(new Candidate(j, b, sortedCoins[j], score));
        }

        // 3) Einmalig nach Score absteigend sortieren
        cand.sort((a, b) -> Double.compare(b.score, a.score));

        // 4) Registration in dieser Reihenfolge abarbeiten, bis keine Zeit mehr bleibt
        boolean[] scanned = new boolean[coinValues.length];
        int currentTime = 0;
        List<OutputBank> output = new ArrayList<>();

        for (var c : cand) {
            Bank b = c.bank;
            int regDays = b.bankDays();
            if (currentTime + regDays >= days) break;  // keine Zeit mehr für Registrierung

            // verfügbare Scan-Tage danach
            currentTime += regDays;
            int availDays = days - currentTime;
            int cap = Math.min(b.transactionCount() * availDays, c.coins.length);

            List<Integer> toScan = new ArrayList<>(cap);
            int taken = 0;
            for (int id : c.coins) {
                if (!scanned[id]) {
                    scanned[id] = true;
                    toScan.add(id);
                    if (++taken == cap) break;
                }
            }
            if (!toScan.isEmpty()) {
                output.add(new OutputBank(c.idx, toScan));
            }
        }

        // 5) Ausgabe
        saver.append(String.valueOf(output.size()));
        for (OutputBank ob : output) {
            saver.append(ob.bankId + " " + ob.coins.size());
            for (int coin : ob.coins) {
                saver.appendIn(String.valueOf(coin));
            }
            saver.append("");
        }
    }

    private record OutputBank(int bankId, List<Integer> coins) {}
}