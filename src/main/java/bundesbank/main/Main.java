package bundesbank.main;

import bundesbank.algo.Greedy;
import bundesbank.file.FileReader;
import bundesbank.file.FileSaver;
import bundesbank.file.Task;

import java.util.Arrays;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Bitte geben Sie die Aufgabe ein (A-F):");
        String taskInput = scanner.nextLine().trim().toLowerCase();
        String fileName = switch (taskInput) {
            case "a" -> "a_example.txt";
            case "b" -> "b_count_on.txt";
            case "c" -> "c_incunabula.txt";
            case "d" -> "d_tough_choices.txt";
            case "e" -> "e_so_many_coins.txt";
            case "f" -> "f_banks_of_the_world.txt";
            default -> "";
        };

        // Read the file and parse the data
        Task task = FileReader.parseFile(fileName);

        // Print the parsed data
        System.out.println("Coins: " + task.coins());
        System.out.println("Banks: " + task.banks());
        System.out.println("Days: " + task.days());

        Arrays.stream(task.coinValues()).forEach(value -> System.out.print(value + " "));

        System.out.println("\nBank List:");
        task.bankList().forEach(bank -> {
            System.out.println("Bank Coins: " + bank.bankCoins());
            System.out.println("Bank Days: " + bank.bankDays());
            System.out.println("Transaction Count: " + bank.transactionCount());
            System.out.print("Bank Coin IDs: ");
            Arrays.stream(bank.bankCoinIDs()).forEach(id -> System.out.print(id + " "));
            System.out.println();
        });

        Greedy.solve(task, task.days(), new FileSaver(fileName));
    }
}
