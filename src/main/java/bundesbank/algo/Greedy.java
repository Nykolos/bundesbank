package bundesbank.algo;

import bundesbank.file.Bank;
import bundesbank.file.FileSaver;
import bundesbank.file.Task;

import java.util.*;

public class Greedy {
    // Globale Referenz auf Münzwerte für Candidate
    private static int[] coinValues;

    public static void solve(Task task, int days, FileSaver saver) {
        List<Bank> banks = task.bankList();
        coinValues = task.coinValues();
        int M = coinValues.length;

        // Für schnelle Duplikat-Erkennung
        boolean[] scanned = new boolean[M];

        // Pro Bank: Münzen absteigend nach Wert sortieren
        List<int[]> sortedCoins = new ArrayList<>(banks.size());
        for (Bank b : banks) {
            int[] raw = b.bankCoinIDs();
            Integer[] boxed = Arrays.stream(raw).boxed().toArray(Integer[]::new);
            Arrays.sort(boxed, (a, b2) -> Integer.compare(coinValues[b2], coinValues[a]));
            int[] ids = Arrays.stream(boxed).mapToInt(Integer::intValue).toArray();
            sortedCoins.add(ids);
        }

        // Priority-Queue mit Lazy-Update: sortiert nach aktuellem Score
        PriorityQueue<Candidate> pq = new PriorityQueue<>();
        for (int j = 0; j < banks.size(); j++) {
            pq.offer(new Candidate(j, banks.get(j), sortedCoins.get(j)));
        }

        int currentTime = 0;           // Kalender für Registrierungsende
        List<OutputBank> output = new ArrayList<>();

        // Solange noch Registrierung möglich und Kandidaten existieren
        while (currentTime < days && !pq.isEmpty()) {
            Candidate best;
            // Lazy-Pop/Update
            while (true) {
                best = pq.poll();
                if (best == null) break;

                int availForScore = days - (currentTime + best.bank.bankDays());
                if (availForScore <= 0) {
                    best.currentScore = 0;
                } else {
                    best.recalc(currentTime, days, scanned);
                }

                Candidate next = pq.peek();
                if (next == null || best.currentScore >= next.currentScore) {
                    break;
                }
                pq.offer(best);
            }

            if (best == null || best.currentScore <= 0) {
                break;
            }

            // 1) Registrierung abschließen
            currentTime += best.bank.bankDays();
            // 2) Rest-Tage prüfen
            int availDays = days - currentTime;
            if (availDays <= 0) {
                break;  // keine Scan-Tage mehr übrig
            }

            // 3) Kapazität berechnen (long zur Vermeidung von Überlauf)
            long rawCap = (long) best.bank.transactionCount() * availDays;
            int cap = (int) Math.min(rawCap, best.coinIDs.length);
            cap = Math.max(0, cap);  // sicherstellen, dass cap ≥ 0 ist

            // 4) Münzen zum Scannen auswählen
            List<Integer> toScan = new ArrayList<>(cap);
            int taken = 0;
            for (int coinId : best.coinIDs) {
                if (!scanned[coinId]) {
                    scanned[coinId] = true;
                    toScan.add(coinId);
                    if (++taken == cap) break;
                }
            }

            if (!toScan.isEmpty()) {
                output.add(new OutputBank(best.bankIndex, toScan));
            }
            // best.bank wird nicht zurück in pq gegeben, da abgearbeitet
        }

        // Ausgabe formatieren
        saver.append(String.valueOf(output.size()));
        for (OutputBank ob : output) {
            saver.append(ob.bankId + " " + ob.coins.size());
            for (int c : ob.coins) {
                saver.appendIn(String.valueOf(c));
            }
            saver.append("");
        }
    }

    private static class Candidate implements Comparable<Candidate> {
        final int bankIndex;
        final Bank bank;
        final int[] coinIDs;
        double currentScore = 0;

        Candidate(int bankIndex, Bank bank, int[] sortedCoins) {
            this.bankIndex = bankIndex;
            this.bank = bank;
            this.coinIDs = sortedCoins;
        }

        void recalc(int currentTime, int totalDays, boolean[] scanned) {
            int regEnd = currentTime + bank.bankDays();
            int availDays = totalDays - regEnd;
            if (availDays <= 0) {
                currentScore = 0;
                return;
            }
            int cap = Math.min(bank.transactionCount() * availDays, coinIDs.length);
            long sum = 0;
            int taken = 0;
            for (int id : coinIDs) {
                if (!scanned[id]) {
                    sum += coinValues[id];
                    if (++taken == cap) break;
                }
            }
            currentScore = (double) sum / bank.bankDays();
        }

        @Override
        public int compareTo(Candidate o) {
            return Double.compare(o.currentScore, this.currentScore);
        }
    }

    private record OutputBank(int bankId, List<Integer> coins) {}
}