//
// Copyright (c) 2016-2025 Deephaven Data Labs and Patent Pending
//
// ****** AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY
// ****** Edit TupleSourceCodeGenerator and run "./gradlew replicateTupleSources" to regenerate
//
// @formatter:off
package io.deephaven.engine.table.impl.tuplesource.generated;

import io.deephaven.chunk.ByteChunk;
import io.deephaven.chunk.Chunk;
import io.deephaven.chunk.IntChunk;
import io.deephaven.chunk.ObjectChunk;
import io.deephaven.chunk.WritableChunk;
import io.deephaven.chunk.WritableObjectChunk;
import io.deephaven.chunk.attributes.Values;
import io.deephaven.engine.table.ColumnSource;
import io.deephaven.engine.table.TupleSource;
import io.deephaven.engine.table.WritableColumnSource;
import io.deephaven.engine.table.impl.tuplesource.AbstractTupleSource;
import io.deephaven.engine.table.impl.tuplesource.ThreeColumnTupleSourceFactory;
import io.deephaven.tuple.generated.IntByteObjectTuple;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Integer, Byte, and Object.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class IntegerByteObjectColumnTupleSource extends AbstractTupleSource<IntByteObjectTuple> {

    /** {@link ThreeColumnTupleSourceFactory} instance to create instances of {@link IntegerByteObjectColumnTupleSource}. **/
    public static final ThreeColumnTupleSourceFactory<IntByteObjectTuple, Integer, Byte, Object> FACTORY = new Factory();

    private final ColumnSource<Integer> columnSource1;
    private final ColumnSource<Byte> columnSource2;
    private final ColumnSource<Object> columnSource3;

    public IntegerByteObjectColumnTupleSource(
            @NotNull final ColumnSource<Integer> columnSource1,
            @NotNull final ColumnSource<Byte> columnSource2,
            @NotNull final ColumnSource<Object> columnSource3
    ) {
        super(columnSource1, columnSource2, columnSource3);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
        this.columnSource3 = columnSource3;
    }

    @Override
    public final IntByteObjectTuple createTuple(final long rowKey) {
        return new IntByteObjectTuple(
                columnSource1.getInt(rowKey),
                columnSource2.getByte(rowKey),
                columnSource3.get(rowKey)
        );
    }

    @Override
    public final IntByteObjectTuple createPreviousTuple(final long rowKey) {
        return new IntByteObjectTuple(
                columnSource1.getPrevInt(rowKey),
                columnSource2.getPrevByte(rowKey),
                columnSource3.getPrev(rowKey)
        );
    }

    @Override
    public final IntByteObjectTuple createTupleFromValues(@NotNull final Object... values) {
        return new IntByteObjectTuple(
                TypeUtils.unbox((Integer)values[0]),
                TypeUtils.unbox((Byte)values[1]),
                values[2]
        );
    }

    @Override
    public final IntByteObjectTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new IntByteObjectTuple(
                TypeUtils.unbox((Integer)values[0]),
                TypeUtils.unbox((Byte)values[1]),
                values[2]
        );
    }

    @Override
    public final int tupleLength() {
        return 3;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final IntByteObjectTuple tuple, final int elementIndex, @NotNull final WritableColumnSource<ELEMENT_TYPE> writableSource, final long destinationRowKey) {
        if (elementIndex == 0) {
            writableSource.set(destinationRowKey, tuple.getFirstElement());
            return;
        }
        if (elementIndex == 1) {
            writableSource.set(destinationRowKey, tuple.getSecondElement());
            return;
        }
        if (elementIndex == 2) {
            writableSource.set(destinationRowKey, (ELEMENT_TYPE) tuple.getThirdElement());
            return;
        }
        throw new IndexOutOfBoundsException("Invalid element index " + elementIndex + " for export");
    }

    @Override
    public final Object exportElement(@NotNull final IntByteObjectTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        if (elementIndex == 2) {
            return tuple.getThirdElement();
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }

    @Override
    public final void exportAllTo(final Object @NotNull [] dest, @NotNull final IntByteObjectTuple tuple) {
        dest[0] = TypeUtils.box(tuple.getFirstElement());
        dest[1] = TypeUtils.box(tuple.getSecondElement());
        dest[2] = tuple.getThirdElement();
    }

    @Override
    public final void exportAllTo(final Object @NotNull [] dest, @NotNull final IntByteObjectTuple tuple, final int @NotNull [] map) {
        dest[map[0]] = TypeUtils.box(tuple.getFirstElement());
        dest[map[1]] = TypeUtils.box(tuple.getSecondElement());
        dest[map[2]] = tuple.getThirdElement();
    }

    @Override
    public final Object exportElementReinterpreted(@NotNull final IntByteObjectTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return TypeUtils.box(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        if (elementIndex == 2) {
            return tuple.getThirdElement();
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }
    @Override
    public final void exportAllReinterpretedTo(final Object @NotNull [] dest, @NotNull final IntByteObjectTuple tuple) {
        dest[0] = TypeUtils.box(tuple.getFirstElement());
        dest[1] = TypeUtils.box(tuple.getSecondElement());
        dest[2] = tuple.getThirdElement();
    }

    @Override
    public final void exportAllReinterpretedTo(final Object @NotNull [] dest, @NotNull final IntByteObjectTuple tuple, final int @NotNull [] map) {
        dest[map[0]] = TypeUtils.box(tuple.getFirstElement());
        dest[map[1]] = TypeUtils.box(tuple.getSecondElement());
        dest[map[2]] = tuple.getThirdElement();
    }


    @Override
    protected void convertChunks(@NotNull WritableChunk<? super Values> destination, int chunkSize, Chunk<? extends Values> [] chunks) {
        WritableObjectChunk<IntByteObjectTuple, ? super Values> destinationObjectChunk = destination.asWritableObjectChunk();
        IntChunk<? extends Values> chunk1 = chunks[0].asIntChunk();
        ByteChunk<? extends Values> chunk2 = chunks[1].asByteChunk();
        ObjectChunk<Object, ? extends Values> chunk3 = chunks[2].asObjectChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new IntByteObjectTuple(chunk1.get(ii), chunk2.get(ii), chunk3.get(ii)));
        }
        destinationObjectChunk.setSize(chunkSize);
    }

    /** {@link ThreeColumnTupleSourceFactory} for instances of {@link IntegerByteObjectColumnTupleSource}. **/
    private static final class Factory implements ThreeColumnTupleSourceFactory<IntByteObjectTuple, Integer, Byte, Object> {

        private Factory() {
        }

        @Override
        public TupleSource<IntByteObjectTuple> create(
                @NotNull final ColumnSource<Integer> columnSource1,
                @NotNull final ColumnSource<Byte> columnSource2,
                @NotNull final ColumnSource<Object> columnSource3
        ) {
            return new IntegerByteObjectColumnTupleSource(
                    columnSource1,
                    columnSource2,
                    columnSource3
            );
        }
    }
}
