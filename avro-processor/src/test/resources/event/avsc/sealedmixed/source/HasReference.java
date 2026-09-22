package event.avsc.sealedmixed;

/** Plain (non-@Event) supertype interface, mirroring a shared cross-cutting contract. */
public interface HasReference {
    String reference();

    default String referenceLabel() {
        return "ref:" + reference();
    }
}
