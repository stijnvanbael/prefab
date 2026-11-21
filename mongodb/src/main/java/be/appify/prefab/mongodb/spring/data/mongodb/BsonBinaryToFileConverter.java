package be.appify.prefab.mongodb.spring.data.mongodb;

import be.appify.prefab.core.binary.ByteArrayToFileConverter;
import java.io.File;
import org.bson.types.Binary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/** Converter to transform binary data read from MongoDB into a temporary File. */
@ReadingConverter
public class BsonBinaryToFileConverter implements Converter<Binary, File> {
    private final ByteArrayToFileConverter byteArrayToFile = new ByteArrayToFileConverter();

    /** Constructs a new BsonBinaryToFileConverter. */
    public BsonBinaryToFileConverter() {
    }

    @Override
    public File convert(Binary source) {
        return byteArrayToFile.convert(source.getData());
    }
}
