package be.appify.prefab.example.streams;

sealed interface ClassifiedWordEvent permits ShortWordEvent, LongWordEvent {
    String id();

    String word();
}

