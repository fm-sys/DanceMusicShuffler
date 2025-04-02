package org.example.util;

import java.util.LinkedList;

public class FixedSizeQueue<T> extends LinkedList<T> {
    private int maxSize;

    public FixedSizeQueue(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public boolean add(T item) {
        if (maxSize == 0 && size() == 0) {
            return false; // In diesem Fall können wir weder ein Element hinzufügen noch entfernen
        }
        if (size() >= maxSize) {
            removeFirst(); // Entfernt das älteste Element (FIFO)
        }
        return super.add(item);
    }

    @Override
    public boolean offer(T item) {
        return add(item); // Gleiche Logik wie add()
    }

    public void setMaxSize(int newSize) {
        this.maxSize = newSize;
        while (size() > maxSize) {
            removeFirst(); // Entfernt überschüssige Elemente, falls die Größe verringert wurde
        }
    }

    public int getMaxSize() {
        return maxSize;
    }

    public static void main(String[] args) {
        FixedSizeQueue<String> queue = new FixedSizeQueue<>(3);

        queue.add("A");
        queue.add("B");
        queue.add("C");
        System.out.println(queue); // [A, B, C]

        queue.add("D"); // A wird entfernt, weil die Größe 3 ist
        System.out.println(queue); // [B, C, D]

        queue.setMaxSize(2); // Größe verringern, überschüssige Elemente werden entfernt
        System.out.println(queue); // [C, D]
    }
}
