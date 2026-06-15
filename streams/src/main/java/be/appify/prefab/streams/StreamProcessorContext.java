package be.appify.prefab.streams;

public interface StreamProcessorContext<K, VO> {
    void forward(StreamRecord<K, VO> value);
}
