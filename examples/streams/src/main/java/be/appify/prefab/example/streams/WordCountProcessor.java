package be.appify.prefab.example.streams;

import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StatefulStreamProcessor;
import be.appify.prefab.streams.Store;
import be.appify.prefab.streams.StreamRecord;

public class WordCountProcessor extends StatefulStreamProcessor<WordEvent, WordEvent> {

    private final Store<WordCount> wordCountStore;

    protected WordCountProcessor(PrefabStreams context) {
        super(context, WordCount.class);
        wordCountStore = store(WordCount.class);
    }

    public void process(StreamRecord<WordEvent> streamRecord) {
        var wordCount = wordCountStore.get(streamRecord.key())
                .map(wc -> new WordCount(wc.word(), wc.count() + 1))
                .orElseGet(() -> new WordCount(streamRecord.value().word(), 1));

        wordCountStore.put(streamRecord.key(), wordCount);
        forward(streamRecord);
    }
}
