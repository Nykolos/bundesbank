package bundesbank.algo;

import bundesbank.file.Bank;
import bundesbank.file.FileSaver;
import bundesbank.file.Task;

import java.util.*;

/**
 * Greedy-Algorithmus mit mehrfachen Starts und leichtem Zufalls-Tiebreaking,
 * um aus schwierigen Fällen höhere Punktzahlen zu erreichen.
 */
public class Greedy {
    private static int[] coinValues;

    public static void solve(Task task, int days, FileSaver saver) {
        List<Bank> banks = task.bankList();
        coinValues = task.coinValues();
        int M = coinValues.length;
        int B = banks.size();

        // 0) Vorberechnung: Für jede Bank Münzen nach Wert absteigend sortieren
        List<int[]> sortedCoins = new ArrayList<>(B);
        for (Bank b : banks) {
            int[] raw = b.bankCoinIDs();
            Integer[] boxed = Arrays.stream(raw).boxed().toArray(Integer[]::new);
            Arrays.sort(boxed, (a, b2) -> Integer.compare(coinValues[b2], coinValues[a]));
            int[] sorted = Arrays.stream(boxed).mapToInt(i -> i).toArray();
            sortedCoins.add(sorted);
        }

        // 1) Mehrere Greedy-Starts mit leichtem Zufalls-Tiebreaking
        int runs = 10;
        Random runRnd = new Random(42);
        List<OutputBank> bestOverall = null;
        int bestScore = -1;

        for (int run = 0; run < runs; run++) {
            boolean[] scanned = new boolean[M];
            PriorityQueue<Candidate> pq = buildQueue(banks, sortedCoins, days, scanned, runRnd);

            int currentTime = 0;
            List<OutputBank> output = new ArrayList<>();

            // 2) Standard-Greedy mit lazy Recalc
            while (currentTime < days && !pq.isEmpty()) {
                Candidate best = popBest(pq, currentTime, days, scanned, runRnd);
                if (best == null || best.currentScore <= 0) break;

                // Registrierung
                currentTime += best.bank.bankDays();
                int availDays = days - currentTime;
                if (availDays <= 0) break;

                // Kapazität
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
            }

            // 3) Score simulieren und besten Lauf merken
            int score = simulateScore(output, coinValues, banks, days);
            if (score > bestScore) {
                bestScore = score;
                bestOverall = output;
            }
        }

        // 4) Ausgabe der besten gefundene Lösung
        saver.append(String.valueOf(bestOverall.size()));
        for (OutputBank ob : bestOverall) {
            saver.append(ob.bankId + " " + ob.coins.size());
            for (int c : ob.coins) {
                saver.appendIn(String.valueOf(c));
            }
            saver.append("");
        }
    }

    /** Baut die initiale PriorityQueue für einen Lauf auf */
    private static PriorityQueue<Candidate> buildQueue(
            List<Bank> banks,
            List<int[]> sortedCoins,
            int days,
            boolean[] scanned,
            Random rnd) {
        PriorityQueue<Candidate> pq = new PriorityQueue<>();
        for (int i = 0; i < banks.size(); i++) {
            Candidate c = new Candidate(i, banks.get(i), sortedCoins.get(i));
            c.recalc(0, days, scanned, rnd);
            if (c.currentScore > 0) {
                pq.offer(c);
            }
        }
        return pq;
    }

    /** Poppt das beste Candidate-Objekt mit lazy Recalc und Zufallsjusierung */
    private static Candidate popBest(
            PriorityQueue<Candidate> pq,
            int currentTime,
            int days,
            boolean[] scanned,
            Random rnd) {
        while (!pq.isEmpty()) {
            Candidate best = pq.poll();
            best.recalc(currentTime, days, scanned, rnd);
            Candidate next = pq.peek();
            if (next == null || best.currentScore + 1e-8 >= next.currentScore) {
                return best;
            }
            pq.offer(best);
        }
        return null;
    }

    /** Simuliert den exakten Score einer Lösungsfolge */
    private static int simulateScore(
            List<OutputBank> seq,
            int[] coinValues,
            List<Bank> banks,
            int totalDays) {
        boolean[] seen = new boolean[coinValues.length];
        int time = 0, score = 0;
        for (OutputBank ob : seq) {
            Bank b = banks.get(ob.bankId);
            time += b.bankDays();
            int avail = totalDays - time;
            if (avail <= 0) break;
            int cap = Math.min(b.transactionCount() * avail, ob.coins.size());
            int taken = 0;
            for (int c : ob.coins) {
                if (!seen[c]) {
                    seen[c] = true;
                    score += coinValues[c];
                    if (++taken == cap) break;
                }
            }
        }
        return score;
    }

    private static class Candidate implements Comparable<Candidate> {
        final int bankIndex;
        final Bank bank;
        final int[] coinIDs;
        double currentScore;

        Candidate(int bankIndex, Bank bank, int[] coinIDs) {
            this.bankIndex = bankIndex;
            this.bank = bank;
            this.coinIDs = coinIDs;
        }

        /**
         * Berechnet currentScore = (Summe der ungescannten Top-cap-Münzen) / Registrierungsdauer
         * plus eine kleine Zufallskomponente zum Tiebreaking.
         */
        void recalc(int currentTime, int totalDays, boolean[] scanned, Random rnd) {
            int regDays = bank.bankDays();
            int endReg = currentTime + regDays;
            int avail = totalDays - endReg;
            if (avail <= 0) {
                currentScore = 0;
                return;
            }

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
            if (sum <= 0) {
                currentScore = 0;
            } else {
                currentScore = (double) sum / regDays
                        + rnd.nextDouble() * 1e-8;
            }
        }

        @Override
        public int compareTo(Candidate o) {
            return Double.compare(o.currentScore, this.currentScore);
        }
    }

    private record OutputBank(int bankId, List<Integer> coins) {}
}