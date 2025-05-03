package bundesbank.algo;

import bundesbank.file.Bank;
import bundesbank.file.FileSaver;
import bundesbank.file.Task;

import java.util.*;

public class Greedy {
    public static void solve(Task task, int days, FileSaver saver) {
        Set<Integer> scannedCoins = new HashSet<>();
        int currentDay = 0;
        List<OutputBank> output = new ArrayList<>();
        List<Bank> availableBanks = new ArrayList<>(task.bankList());

        // Sortiere Banken nach Effizienzpotential (vorberechnet)
        Map<Bank, Double> bankPotential = new HashMap<>();
        for (Bank bank : availableBanks) {
            int uniqueCoins = 0;
            int totalValue = 0;
            Set<Integer> seen = new HashSet<>();

            for (int coinId : bank.bankCoinIDs()) {
                if (!seen.contains(coinId)) {
                    totalValue += task.coinValues()[coinId];
                    uniqueCoins++;
                    seen.add(coinId);
                }
            }

            // Potential-Score: Wert pro Tag unter idealen Bedingungen
            double potential = (double) totalValue / (bank.bankDays() +
                    Math.ceil((double)uniqueCoins / bank.transactionCount()));
            bankPotential.put(bank, potential);
        }

        while (currentDay < days && !availableBanks.isEmpty()) {
            double bestScore = -1;
            int bestBankIndex = -1;
            List<Integer> bestCoins = null;
            int bestDaysUsed = 0;

            // Bewerte jede Bank mit tieferem Lookahead (bis zu 3 Banken)
            for (int i = 0; i < availableBanks.size(); i++) {
                Bank bank = availableBanks.get(i);
                int remainingDays = days - currentDay;

                if (bank.bankDays() >= remainingDays) continue;

                // Simuliere diese Bank
                Set<Integer> tempScanned = new HashSet<>(scannedCoins);
                List<Integer> bankCoins = simulateCollection(bank, remainingDays - bank.bankDays(),
                        tempScanned, task.coinValues());

                if (bankCoins.isEmpty()) continue;

                // Berechne Gesamtzeit und Wert
                int totalValue = calculateValue(bankCoins, task.coinValues());
                int samplingDays = calculateSamplingDays(bankCoins.size(), bank.transactionCount());
                int totalDays = bank.bankDays() + samplingDays;

                // Start-Score dieser Bank
                double baseScore = (double) totalValue / totalDays;

                // Rekursive Bewertung mit Lookahead (bis zu 2 weitere Banken)
                double lookaheadBonus = evaluateLookahead(
                        availableBanks, i, currentDay + totalDays,
                        new HashSet<>(tempScanned), task, days, 0, 2);

                // Gewichteter Gesamt-Score mit abnehmendem Gewicht für weitere Lookaheads
                double finalScore = baseScore + lookaheadBonus;

                if (finalScore > bestScore) {
                    bestScore = finalScore;
                    bestBankIndex = i;
                    bestCoins = bankCoins;
                    bestDaysUsed = totalDays;
                }
            }

            if (bestBankIndex == -1) break;

            // Gewählte Bank anwenden
            Bank chosenBank = availableBanks.get(bestBankIndex);
            output.add(new OutputBank(task.bankList().indexOf(chosenBank), bestCoins));
            scannedCoins.addAll(bestCoins);
            currentDay += bestDaysUsed;
            availableBanks.remove(bestBankIndex);
        }

        // Output formatieren
        saver.append(String.valueOf(output.size()));
        for (OutputBank ob : output) {
            saver.append(ob.bankId + " " + ob.coins.size());
            for (Integer coin : ob.coins) {
                saver.appendIn(coin.toString());
            }
            saver.append("");
        }
    }

    // Rekursive Lookahead-Bewertung
    private static double evaluateLookahead(List<Bank> banks, int currentBankIndex,
                                            int startDay, Set<Integer> scannedCoins,
                                            Task task, int totalDays, int depth, int maxDepth) {
        if (depth >= maxDepth || startDay >= totalDays || banks.size() <= 1) {
            return 0;
        }

        double bestNextScore = 0;

        for (int i = 0; i < banks.size(); i++) {
            if (i == currentBankIndex) continue;

            Bank nextBank = banks.get(i);
            int remainingDays = totalDays - startDay;

            if (nextBank.bankDays() >= remainingDays) continue;

            // Simuliere die nächste Bank
            Set<Integer> tempScanned = new HashSet<>(scannedCoins);
            List<Integer> nextCoins = simulateCollection(nextBank,
                    remainingDays - nextBank.bankDays(), tempScanned, task.coinValues());

            if (nextCoins.isEmpty()) continue;

            int nextValue = calculateValue(nextCoins, task.coinValues());
            int samplingDays = calculateSamplingDays(nextCoins.size(), nextBank.transactionCount());
            int nextTotalDays = nextBank.bankDays() + samplingDays;

            double nextScore = (double) nextValue / nextTotalDays;

            // Tieferer Lookahead mit abnehmender Gewichtung
            double deeperLookahead = evaluateLookahead(
                    banks, i, startDay + nextTotalDays,
                    new HashSet<>(tempScanned), task, totalDays, depth + 1, maxDepth);

            double weightFactor = 1.0 / (depth + 1);
            double combinedScore = nextScore + deeperLookahead * weightFactor;

            bestNextScore = Math.max(bestNextScore, combinedScore);
        }

        return bestNextScore;
    }

    private static List<Integer> simulateCollection(Bank bank, int availableDays,
                                                    Set<Integer> scannedCoins, int[] coinValues) {
        List<Integer> coins = new ArrayList<>();
        int maxCoins = Math.min(bank.transactionCount() * availableDays, bank.bankCoinIDs().length);

        for (int i = 0; i < maxCoins; i++) {
            int coinId = bank.bankCoinIDs()[i];
            if (!scannedCoins.contains(coinId)) {
                coins.add(coinId);
                scannedCoins.add(coinId);
            }
        }

        return coins;
    }

    private static int calculateValue(List<Integer> coins, int[] coinValues) {
        int value = 0;
        for (Integer coinId : coins) {
            value += coinValues[coinId];
        }
        return value;
    }

    private static int calculateSamplingDays(int coinCount, int transactionsPerDay) {
        return (int) Math.ceil((double) coinCount / transactionsPerDay);
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