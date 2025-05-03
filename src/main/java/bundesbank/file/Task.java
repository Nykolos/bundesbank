package bundesbank.file;

import java.util.ArrayList;

public record Task(int coins, int banks, int days, int[] coinValues, ArrayList<Bank> bankList) {}

