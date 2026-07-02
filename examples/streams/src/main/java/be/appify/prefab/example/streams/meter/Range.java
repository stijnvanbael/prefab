package be.appify.prefab.example.streams.meter;

public record Range<T extends Comparable<T>>(T start, T end) {
    public boolean contains(T value) {
        return start.compareTo(value) <= 0 && end.compareTo(value) >= 0;
    }
}
