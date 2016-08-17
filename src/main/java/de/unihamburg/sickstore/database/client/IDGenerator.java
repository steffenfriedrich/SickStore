package de.unihamburg.sickstore.database.client;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;


class IDGenerator {
    private static final long MAX_UNSIGNED_LONG = -1L;

    static IDGenerator newInstance() {
        return new IDGenerator(2);
    }

    private final AtomicLongArray bits;
    private final int maxIds;
    private final AtomicInteger offset;

    // If a query timeout, we'll stop waiting for it. However in that case, we
    // can't release/reuse the ID because we don't know if the response is lost
    // or will just come back to use sometimes in the future. In that case, we
    // just "mark" the fact that we have one less available ID and marked counts
    // how many marks we've put.
    private final AtomicInteger marked = new AtomicInteger(0);

    private IDGenerator(int streamIdSizeInBytes) {
        // Stream IDs are signed and we only handle positive values
        // (negative stream IDs are for server side initiated streams).
        maxIds = 1 << (streamIdSizeInBytes * 8 - 1);

        // This is true for 1 byte = 128 streams, and therefore for any higher value
        assert maxIds % 64 == 0;

        // We use one bit in our array of longs to represent each stream ID.
        bits = new AtomicLongArray(maxIds / 64);

        // Initialize all bits to 1
        for (int i = 0; i < bits.length(); i++)
            bits.set(i, MAX_UNSIGNED_LONG);

        offset = new AtomicInteger(bits.length() - 1);
    }

    public int next() {
        int previousOffset, myOffset;
        do {
            previousOffset = offset.get();
            myOffset = (previousOffset + 1) % bits.length();
        } while (!offset.compareAndSet(previousOffset, myOffset));

        for (int i = 0; i < bits.length(); i++) {
            int j = (i + myOffset) % bits.length();

            int id = atomicGetAndSetFirstAvailable(j);
            if (id >= 0)
                return id + (64 * j);
        }
        return -1;
    }

    public void release(int streamId) {
        atomicClear(streamId / 64, streamId % 64);
    }

    public void mark(int streamId) {
        marked.incrementAndGet();
    }

    public void unmark(int streamId) {
        marked.decrementAndGet();
    }

    public int maxAvailableStreams() {
        return maxIds - marked.get();
    }

    // Returns >= 0 if found and set an id, -1 if no bits are available.
    private int atomicGetAndSetFirstAvailable(int idx) {
        while (true) {
            long l = bits.get(idx);
            if (l == 0)
                return -1;

            // Find the position of the right-most 1-bit
            int id = Long.numberOfTrailingZeros(l);
            if (bits.compareAndSet(idx, l, l ^ mask(id)))
                return id;
        }
    }

    private void atomicClear(int idx, int toClear) {
        while (true) {
            long l = bits.get(idx);
            if (bits.compareAndSet(idx, l, l | mask(toClear)))
                return;
        }
    }

    private static long mask(int id) {
        return 1L << id;
    }
}
