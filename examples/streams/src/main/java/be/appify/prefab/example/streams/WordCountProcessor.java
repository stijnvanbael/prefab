package be.appify.prefab.example.streams;

import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StatefulStreamProcessor;
import be.appify.prefab.streams.StreamRecord;

public class WordCountProcessor extends StatefulStreamProcessor<Word, WordEvent, Word, WordEvent> {

    protected WordCountProcessor(PrefabStreams context) {
        super(WordCount.class);
    }

    public void process(StreamRecord<Word, WordEvent> streamRecord) {
        var wordCountStore = store(WordCount.class);
        var wordCount = wordCountStore.get(streamRecord.key())
                .map(wc -> new WordCount(wc.key(), wc.count() + 1))
                .orElseGet(() -> new WordCount(streamRecord.value().word(), 1));

        wordCountStore.put(streamRecord.key(), wordCount);
        forward(streamRecord);
    }
}
