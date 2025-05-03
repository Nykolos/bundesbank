package bundesbank.algo;

import bundesbank.file.Bank;
import bundesbank.file.FileSaver;
import bundesbank.file.Task;

import java.util.*;

public class Greedy {
    public static void solve(Task task, int days, FileSaver saver) {
        Set<Integer> scannedCoins = new HashSet<>();
        int currentDay = 0;

        // Banken mit ihrer original ID und Effizienzwert vorbereiten
        List<BankWithScore> banksWithScores = new ArrayList<>();
        for (int i = 0; i < task.bankList().size(); i++) {
            Bank bank = task.bankList().get(i);
            int value = 0;
            for (int coinID : bank.bankCoinIDs()) {
                value += task.coinValues()[coinID];
            }
            double score = (double) value / bank.bankDays();
            banksWithScores.add(new BankWithScore(i, bank, score));
        }

        // Nach Effizienz sortieren
        banksWithScores.sort((b1, b2) -> Double.compare(b2.score, b1.score));

        List<OutputBank> output = new ArrayList<>();

        for (BankWithScore bankWithScore : banksWithScores) {
            Bank bank = bankWithScore.bank;
            int remainingDays = days - currentDay;
            if (remainingDays <= 0) break; // Früher abbrechen, wenn keine Tage übrig

            int usableDays = Math.min(remainingDays, bank.bankDays());
            List<Integer> coinsToSend = new ArrayList<>();
            int index = 0;

            for (int d = 0; d < usableDays; d++) {
                for (int i = 0; i < bank.transactionCount() && index < bank.bankCoinIDs().length; i++) {
                    int coinId = bank.bankCoinIDs()[index++];
                    if (!scannedCoins.contains(coinId)) {
                        coinsToSend.add(coinId);
                        scannedCoins.add(coinId);
                    }
                }
            }

            if (!coinsToSend.isEmpty()) {
                output.add(new OutputBank(bankWithScore.originalIndex, coinsToSend));
            }
            currentDay += usableDays;
        }

        // Output erzeugen
        saver.append(String.valueOf(output.size()));
        for (OutputBank ob : output) {
            saver.append(ob.bankId + " " + ob.coins.size());
            ob.coins.forEach(c -> saver.appendIn(c.toString()));
            saver.append("");
        }
    }

    static class BankWithScore {
        int originalIndex;
        Bank bank;
        double score;

        BankWithScore(int originalIndex, Bank bank, double score) {
            this.originalIndex = originalIndex;
            this.bank = bank;
            this.score = score;
        }
    }

    static class OutputBank {
        int bankId;
        List<Integer> coins;

        OutputBank(int bankId, List<Integer> coins) {
            this.bankId = bankId;
            this.coins = coins;
        }
    }
}
