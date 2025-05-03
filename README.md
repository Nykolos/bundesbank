# Bundesbank Workshop 03.05.2025

Hier gab es eine Programmierchallenge in der man eine [Aufgabe](Challenge.pdf) möglichst effizient lösen musste. Hierzu habe ich Java mit einem Greedy Algorithmus genutzt, sowie einen Lazy Recalc und Adjacency-Swap Local Search.


Die Laufzeit beträgt

```text
O(∑ₙ Nⱼ log Nⱼ + B log B + C_total)
 = O(C_total · log N̄ + B · log B + C_total)
