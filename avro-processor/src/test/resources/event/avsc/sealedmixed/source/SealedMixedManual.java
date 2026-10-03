package event.avsc.sealedmixed;

/** Hand-written record variant, not generated from an .avsc schema. */
public record SealedMixedManual(String id, String note, String reference) implements SealedMixedAvsc {
}
