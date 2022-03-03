/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharacterPrimitives and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.libs.primitives;

import io.deephaven.db.tables.dbarrays.DbByteArray;
import io.deephaven.db.tables.dbarrays.DbByteArrayDirect;
import io.deephaven.util.QueryConstants;
import io.deephaven.db.util.LongSizedDataStructure;
import gnu.trove.list.array.TByteArrayList;
import gnu.trove.set.TByteSet;
import gnu.trove.set.hash.TByteHashSet;

import java.util.Arrays;

import static io.deephaven.util.QueryConstants.NULL_BYTE;

/**
 * A set of commonly used functions that can be applied to Byte types.
 */
public class BytePrimitives {
    /**
     * Unboxes a array of values.
     *
     * @param values values.
     * @return unboxed array of values.
     */
    public static byte[] unbox(Byte... values) {
        if(values == null){
            return null;
        }

        byte[] result = new byte[values.length];

        for(int i=0; i<values.length; i++){
            Byte v = values[i];

            if(v == null || isNull(v)) {
                result[i] = NULL_BYTE;
            } else {
                result[i] = v;
            }
        }

        return result;
    }

    /**
     * Determines if a value is null.
     *
     * @param value value.
     * @return true if the value is null, and false otherwise.
     */
    static public boolean isNull(byte value){
        return value == NULL_BYTE;
    }

    /**
     * Replaces null values with a default value.
     *
     * @param value value.
     * @param defaultValue default value to return for null values.
     * @return value, if value is not null, and defaultValue if value is null.
     */
    static public byte nullToValue(byte value, byte defaultValue) {
        if (isNull(value)) {
            return defaultValue;
        } else {
            return value;
        }
    }

    /**
     * Replaces null values with a default value.
     *
     * @param values values.
     * @param defaultValue default value to return for null values.
     * @return values with nulls replaced by defaultValue.
     */
    static public byte[] nullToValue(byte[] values, byte defaultValue) {
        return nullToValue(new DbByteArrayDirect(values), defaultValue);
    }

    /**
     * Replaces null values with a default value.
     *
     * @param values values.
     * @param defaultValue default value to return for null values.
     * @return values with nulls replaced by defaultValue.
     */
    static public byte[] nullToValue(DbByteArray values, byte defaultValue) {
        byte[] result = new byte[LongSizedDataStructure.intSize("nullToValue", values.size())];

        for (int i = 0; i < values.size(); i++) {
            result[i] = nullToValue(values.get(i), defaultValue);
        }

        return result;
    }

    /**
     * Counts the number of non-null values.
     *
     * @param values values.
     * @return number of non-null values.
     */
    static public int count(Byte[] values){
        if (values == null){
            return 0;
        }
        int count = 0;
        for (Byte value : values) {
            if (value != null && !isNull(value)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts the number of non-null values.
     *
     * @param values values.
     * @return number of non-null values.
     */
    static public int count(byte[] values){
        if (values == null){
            return 0;
        }

        return count(new DbByteArrayDirect(values));
    }

    /**
     * Counts the number of non-null values.
     *
     * @param values values.
     * @return number of non-null values.
     */
    static public int count(DbByteArray values){
        if (values == null){
            return 0;
        }
        int count = 0;
        for (int i = 0; i < values.size();i++) {
            if (!isNull(values.get(i))) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the last value from an array.
     *
     * @param values values.
     * @return last value from the array.
     */
    static public byte last(DbByteArray values){
        if(values == null || values.size() < 1){
            return NULL_BYTE;
        }

        return values.get(values.size() - 1);
    }

    /**
     * Returns the last value from an array.
     *
     * @param values values.
     * @return last value from the array.
     */
    static public byte last(byte[] values){
        if(values == null || values.length < 1){
            return NULL_BYTE;
        }

        return values[values.length - 1];
    }

    /**
     * Returns the first value from an array.
     *
     * @param values values.
     * @return first value from the array.
     */
    static public byte first(DbByteArray values){
        if(values == null || values.size() < 1){
            return NULL_BYTE;
        }

        return values.get(0);
    }

    /**
     * Returns the first value from an array.
     *
     * @param values values.
     * @return first value from the array.
     */
    static public byte first(byte[] values){
        if(values == null || values.length < 1){
            return NULL_BYTE;
        }

        return first(array(values));
    }

    /**
     * Returns the nth value from an array.
     *
     * @param index index of the value to return.
     * @param values values.
     * @return nth value from the array or null, if the index is outside of the array's index range.
     */
    static public byte nth(int index, DbByteArray values){
        if(index < 0 || index >= values.size()){
            return NULL_BYTE;
        }

        return values.get(index);
    }

    /**
     * Returns the nth value from an array.
     *
     * @param index index of the value to return.
     * @param values values.
     * @return nth value from the array or null, if the index is outside of the array's index range.
     */
    static public byte nth(int index, byte[] values){
        return nth(index, array(values));
    }

    /**
     * Converts a DB array to a primitive array.
     *
     * @param values DB array
     * @return primitive array.
     */
    public static byte[] vec(DbByteArray values) {
        if(values == null){
            return null;
        }

        return values.toArray();
    }

    /**
     * Converts a primitive array to a DB array.
     *
     * @param values primitive array
     * @return DB array.
     */
    public static DbByteArray array(byte[] values) {
        if(values == null){
            return null;
        }

        return new DbByteArrayDirect(values);
    }

    /**
     * Checks if a value is within a range.
     *
     * @param testedValue tested value.
     * @param lowInclusiveValue lower inclusive bound of the range.
     * @param highInclusiveValue upper inclusive bound of the range.
     * @return true if the tested value is within the range, and false if the tested value is not in the range or is null.
     */
    static public boolean inRange(byte testedValue,byte lowInclusiveValue,byte highInclusiveValue){
        if (isNull(testedValue)) {
            return false;
        }
        if (testedValue >= lowInclusiveValue && testedValue <= highInclusiveValue) {
            return true;
        }
        return false;
    }

    /**
     * Checks if a value is within a discrete set of possible values.
     *
     * @param testedValues tested value.
     * @param possibleValues possible values.
     * @return true if the tested value is contained in the possible values, and false otherwise.
     */
    static public boolean in(byte testedValues,byte... possibleValues){
        for (int i = 0; i < possibleValues.length; i++) {
            if (testedValues == possibleValues[i]){
                return true;
            }
        }
        return false;
    }


    /**
     * Counts the number of distinct elements in the array.
     *
     * @param values values.
     * @return number of distinct non-null values.
     */
    public static long countDistinct(final byte[] values) {
        if(values == null) {
            return QueryConstants.NULL_LONG;
        }

        return countDistinct(new DbByteArrayDirect(values));
    }

    /**
     * Counts the number of distinct elements in the array.
     *
     * @param values values.
     * @return number of distinct non-null values.
     */
    public static long countDistinct(final DbByteArray values) {
        return countDistinct(values, false);
    }

    /**
     * Counts the number of distinct elements in the array.
     *
     * @param values values.
     * @param countNull true to count null values, and false to exclude null values.
     * @return number of distinct values.
     */
    public static long countDistinct(final byte[] values, boolean countNull) {
        if(values == null) {
            return QueryConstants.NULL_LONG;
        }

        return countDistinct(new DbByteArrayDirect(values), countNull);
    }

    /**
     * Counts the number of distinct elements in the array.
     *
     * @param values values.
     * @param countNull true to count null values, and false to exclude null values.
     * @return number of distinct values.
     */
    public static long countDistinct(final DbByteArray values, boolean countNull) {
        if(values == null) {
            return QueryConstants.NULL_LONG;
        }

        if(values.size() == 0) {
            return 0;
        }

        if(values.size() == 1) {
            return !countNull && values.get(0) == QueryConstants.NULL_BYTE ? 0 : 1;
        }

        final TByteSet keys = new TByteHashSet();
        for(int ii = 0; ii < values.size(); ii++) {
            keys.add(values.get(ii));
        }

        if(!countNull) {
            keys.remove(NULL_BYTE);
        }

        return keys.size();
    }

    /**
     * Get the single unique value in the array, or null if there are none, or there are more than 1 distinct values.
     *
     * @param arr the array
     * @param countNull if nulls should count as values
     * @return the single unique value in the array, or null.
     */
    public static byte uniqueValue(final DbByteArray arr, boolean countNull) {
        if(arr == null || arr.isEmpty()) {
            return NULL_BYTE;
        }

        if(arr.size() == 1) {
            return arr.get(0);
        }

        final TByteSet keys = new TByteHashSet();
        for(int ii = 0; ii < arr.size(); ii++) {
            keys.add(arr.get(ii));
        }

        if(!countNull) {
            keys.remove(NULL_BYTE);
        }

        return keys.size() == 1 ? keys.iterator().next() : NULL_BYTE;
    }

    /**
     * Returns an array containing only the distinct values from the input.
     *
     * @param values values.
     * @return unsorted array containing only distinct non-null items from arr.
     */
    public static byte[] distinct(final byte[] values) {
        if(values == null) {
            return null;
        }

        return distinct(new DbByteArrayDirect(values)).toArray();
    }

    /**
     * Returns an array containing only the distinct values from the input.
     *
     * @param values values.
     * @return unsorted array containing only distinct non-null items from arr.
     */
    public static DbByteArray distinct(final DbByteArray values) {
        if(values == null) {
            return null;
        }

        return distinct(values, false, false);
    }

    /**
     * Returns an array containing only the distinct values from the input.
     *
     * @param values values.
     * @param includeNull true to include null values, and false to exclude null values.
     * @param sort true to sort the resultant array
     * @return array containing only distinct items from arr.
     */
    public static byte[] distinct(final byte[] values, boolean includeNull, boolean sort) {
        if(values == null) {
            return null;
        }

        if(values == null) {
            return null;
        }

        if(values.length == 0) {
            return new byte[0];
        }

        if(values.length == 1) {
            return !includeNull && values[0] == QueryConstants.NULL_BYTE ? new byte[0] : values;
        }

        final TByteArrayList orderedList = new TByteArrayList();
        final TByteSet counts = new TByteHashSet();
        for(int ii = 0; ii < values.length; ii++) {
            byte val = values[ii];
            if((includeNull || val != NULL_BYTE) && counts.add(val)) {
                orderedList.add(val);
            }
        }

        final byte[] data;
        if(sort) {
            orderedList.sort();
            data = orderedList.toArray();
            // region SortFixup
            // endregion SortFixup
        } else {
            data = orderedList.toArray();
        }

        return data;
    }

    /**
     * Returns an array containing only the distinct values from the input.
     *
     * @param values values.
     * @param includeNull true to include null values, and false to exclude null values.
     * @param sort true to sort the resultant array
     * @return array containing only distinct items from arr.
     */
    public static DbByteArray distinct(final DbByteArray values, boolean includeNull, boolean sort) {
        if(values == null) {
            return null;
        }

        if(values.size() == 0) {
            return new DbByteArrayDirect();
        }

        if(values.size() == 1) {
            return !includeNull && values.get(0) == QueryConstants.NULL_BYTE ? new DbByteArrayDirect() : values;
        }

        final TByteArrayList orderedList = new TByteArrayList();
        final TByteSet counts = new TByteHashSet();
        for(int ii = 0; ii < values.size(); ii++) {
            byte val = values.get(ii);
            if((includeNull || val != NULL_BYTE) && counts.add(val)) {
                orderedList.add(val);
            }
        }

        final byte[] data;
        if(sort) {
            orderedList.sort();
            data = orderedList.toArray();
            // region SortFixup
            // endregion SortFixup
        } else {
            data = orderedList.toArray();
        }

        return new DbByteArrayDirect(data);
    }

    /**
     * Returns an array with a value repeated.
     *
     * @param value value.
     * @param size number of times to repeat the value.
     * @return array of repeated values.  If {@code size} is less than zero, an empty array is returned.
     */
    public static byte[] repeat(byte value, int size) {
        if(size < 0){
            return new byte[0];
        }

        final byte[] result = new byte[size];

        for(int i=0; i<size; i++){
            result[i] = value;
        }

        return result;
    }

    /**
     * Returns a list containing its arguments.
     *
     * @param values values.
     * @return list containing values.
     */
    public static byte[] enlist(byte... values){
        if(values == null){
            return new byte[0];
        }

        return values;
    }

    /**
     * Returns the concatenation of multiple arrays into a single array.
     *
     * @param values values.
     * @return concatenation of multiple arrays into a single array.
     */
    public static byte[] concat(byte[]... values){
        if(values == null){
            return new byte[0];
        }

        return concat(Arrays.stream(values).map(e->e==null?null:new DbByteArrayDirect(e)).toArray(DbByteArray[]::new));
    }

    /**
     * Returns the concatenation of multiple arrays into a single array.
     *
     * @param values values.
     * @return concatenation of multiple arrays into a single array.
     */
    public static byte[] concat(DbByteArray... values){
        if(values == null){
            return new byte[0];
        }

        int n = 0;

        for (DbByteArray v : values) {
            if (v != null) {
                n += v.size();
            }
        }

        final byte[] result = new byte[n];
        int idx = 0;

        for (DbByteArray v : values) {
            if (v != null) {
                for (int i = 0; i < v.size(); i++) {
                    result[idx] = v.get(i);
                    idx++;
                }
            }
        }

        return result;
    }

    /**
     * Returns an array with the values reversed.
     *
     * @param values values.
     * @return array with the values reversed.
     */
    public static byte[] reverse(byte... values){
        if(values == null){
            return null;
        }

        return reverse(new DbByteArrayDirect(values));
    }

    /**
     * Returns an array with the values reversed.
     *
     * @param values values.
     * @return array with the values reversed.
     */
    public static byte[] reverse(DbByteArray values){
        if(values == null){
            return null;
        }

        final byte[] result = new byte[(int) values.size()];

        for(int i=0; i<values.size(); i++){
            result[i] = values.get(values.size()-1-i);
        }

        return result;
    }
}