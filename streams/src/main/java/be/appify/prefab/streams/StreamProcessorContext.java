package be.appify.prefab.streams;

public interface StreamProcessorContext<VO> {
    void forward(StreamRecord<VO> value);
}
