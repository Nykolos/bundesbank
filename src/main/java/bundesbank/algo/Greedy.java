package bundesbank.algo;

import bundesbank.file.Bank;
import bundesbank.file.FileSaver;
import bundesbank.file.Task;

import java.util.*;

public class Greedy {
    private static int[] coinValues;

    public static void solve(Task task, int days, FileSaver saver) {
        List<Bank> banks = task.bankList();
        coinValues = task.coinValues();
        int M = coinValues.length;

        // Flags für bereits gescannte Münzen
        boolean[] scanned = new boolean[M];

        // Pro Bank: Münz-IDs absteigend nach Wert sortieren
        List<int[]> sortedCoins = new ArrayList<>(banks.size());
        for (Bank b : banks) {
            int[] raw = b.bankCoinIDs();
            Integer[] boxed = Arrays.stream(raw).boxed().toArray(Integer[]::new);
            Arrays.sort(boxed, (a, b2) -> Integer.compare(coinValues[b2], coinValues[a]));
            sortedCoins.add(Arrays.stream(boxed).mapToInt(i -> i).toArray());
        }

        // 1) Kandidaten erstellen und initial ihren Score bei currentTime=0 berechnen
        PriorityQueue<Candidate> pq = new PriorityQueue<>();
        for (int i = 0; i < banks.size(); i++) {
            Candidate c = new Candidate(i, banks.get(i), sortedCoins.get(i));
            c.recalc(0, days, scanned);   // Initial-Pass
            if (c.currentScore > 0) {
                pq.offer(c);
            }
        }

        int currentTime = 0;
        List<OutputBank> output = new ArrayList<>();

        // 2) Dynamisch picken, lazy nachrechnen, registrieren, scannen
        while (currentTime < days && !pq.isEmpty()) {
            Candidate best;
            // lazy pop & recalc
            while (true) {
                best = pq.poll();
                if (best == null) break;

                // Score neu berechnen mit echtem currentTime
                best.recalc(currentTime, days, scanned);

                Candidate next = pq.peek();
                if (next == null || best.currentScore >= next.currentScore) {
                    break;
                }
                pq.offer(best);
            }
            if (best == null || best.currentScore <= 0) {
                break;
            }

            // Registrierung abschließen
            currentTime += best.bank.bankDays();
            int availDays = days - currentTime;
            if (availDays <= 0) {
                break;
            }

            // Kapazität (long-Zwischenschritt + Clamp ≥0)
            long rawCap = (long) best.bank.transactionCount() * availDays;
            int cap = (int) Math.min(rawCap, best.coinIDs.length);
            cap = Math.max(0, cap);

            // Münzen auswählen
            List<Integer> toScan = new ArrayList<>(cap);
            int taken = 0;
            for (int id : best.coinIDs) {
                if (!scanned[id]) {
                    scanned[id] = true;
                    toScan.add(id);
                    if (++taken == cap) break;
                }
            }
            if (!toScan.isEmpty()) {
                output.add(new OutputBank(best.bankIndex, toScan));
            }
            // Diese Bank kommt nicht mehr zurück in die Queue
        }

        // 3) Ausgabe formatieren
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
        double currentScore;

        Candidate(int bankIndex, Bank bank, int[] sortedCoins) {
            this.bankIndex   = bankIndex;
            this.bank        = bank;
            this.coinIDs     = sortedCoins;
            this.currentScore = 0;
        }

        /**
         * Berechnet currentScore = (Summe der noch ungescannten Top-cap-Münzen) / Registrierungsdauer
         */
        void recalc(int currentTime, int totalDays, boolean[] scanned) {
            int regDays = bank.bankDays();
            int endReg = currentTime + regDays;
            int avail = totalDays - endReg;
            if (avail <= 0) {
                currentScore = 0;
                return;
            }

            // cap = min(P_j * avail, Anzahl Münzen), mit long zum Schutz vor Überlauf
            long raw = (long) bank.transactionCount() * avail;
            int cap = (int) Math.min(raw, coinIDs.length);
            cap = Math.max(0, cap);

            long sum = 0;
            int cnt = 0;
            for (int id : coinIDs) {
                if (!scanned[id]) {
                    sum += coinValues[id];
                    if (++cnt == cap) break;
                }
            }
            currentScore = sum > 0 ? (double) sum / regDays : 0;
        }

        @Override
        public int compareTo(Candidate o) {
            // höchste Scores zuerst
            return Double.compare(o.currentScore, this.currentScore);
        }
    }

    private record OutputBank(int bankId, List<Integer> coins) {}
}