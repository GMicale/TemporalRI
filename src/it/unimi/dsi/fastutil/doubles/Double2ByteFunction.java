/*
	* Copyright (C) 2002-2020 Sebastiano Vigna
	*
	* Licensed under the Apache License, Version 2.0 (the "License");
	* you may not use this file except in compliance with the License.
	* You may obtain a copy of the License at
	*
	*     http://www.apache.org/licenses/LICENSE-2.0
	*
	* Unless required by applicable law or agreed to in writing, software
	* distributed under the License is distributed on an "AS IS" BASIS,
	* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	* See the License for the specific language governing permissions and
	* limitations under the License.
	*/
package it.unimi.dsi.fastutil.doubles;
import it.unimi.dsi.fastutil.Function;
/**
 * A type-specific {@link Function}; provides some additional methods that use
 * polymorphism to avoid (un)boxing.
 *
 * <p>
 * Type-specific versions of {@code get()}, {@code put()} and {@code remove()}
 * cannot rely on {@code null} to denote absence of a key. Rather, they return a
 * {@linkplain #defaultReturnValue() default return value}, which is set to
 * 0/false at creation, but can be changed using the
 * {@code defaultReturnValue()} method.
 *
 * <p>
 * For uniformity reasons, even functions returning objects implement the
 * default return value (of course, in this case the default return value is
 * initialized to {@code null}).
 *
 * <p>
 * The default implementation of optional operations just throw an
 * {@link UnsupportedOperationException}, except for the type-specific {@code
* containsKey()}, which return true. Generic versions of accessors delegate to
 * the corresponding type-specific counterparts following the interface rules.
 *
 * <p>
 * <strong>Warning:</strong> to fall in line as much as possible with the
 * {@linkplain java.util.Map standard map interface}, it is required that
 * standard versions of {@code get()}, {@code put()} and {@code remove()} for
 * maps with primitive-type keys or values <em>return {@code null} to denote
 * missing keys </em> rather than wrap the default return value in an object. In
 * case both keys and values are reference types, the default return value must
 * be returned instead, thus violating the {@linkplain java.util.Map standard
 * map interface} when the default return value is not {@code null}.
 *
 * @see Function
 */
@FunctionalInterface
public interface Double2ByteFunction extends Function<Double, Byte>, java.util.function.DoubleToIntFunction {
	/**
	 * {@inheritDoc}
	 * 
	 * @since 8.0.0
	 */
	@Override
	default int applyAsInt(double operand) {
		return get(operand);
	}
	/**
	 * Adds a pair to the map (optional operation).
	 *
	 * @param key
	 *            the key.
	 * @param value
	 *            the value.
	 * @return the old value, or the {@linkplain #defaultReturnValue() default
	 *         return value} if no value was present for the given key.
	 * @see Function#put(Object,Object)
	 */
	default byte put(final double key, final byte value) {
		throw new UnsupportedOperationException();
	}
	/**
	 * Returns the value to which the given key is mapped.
	 *
	 * @param key
	 *            the key.
	 * @return the corresponding value, or the {@linkplain #defaultReturnValue()
	 *         default return value} if no value was present for the given key.
	 * @see Function#get(Object)
	 */
	byte get(double key);
	/**
	 * Removes the mapping with the given key (optional operation).
	 * 
	 * @param key
	 *            the key.
	 * @return the old value, or the {@linkplain #defaultReturnValue() default
	 *         return value} if no value was present for the given key.
	 * @see Function#remove(Object)
	 */
	default byte remove(final double key) {
		throw new UnsupportedOperationException();
	}
	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated Please use the corresponding type-specific method instead.
	 */
	@Deprecated
	@Override
	default Byte put(final Double key, final Byte value) {
		final double k = (key).doubleValue();
		final boolean containsKey = containsKey(k);
		final byte v = put(k, (value).byteValue());
		return containsKey ? Byte.valueOf(v) : null;
	}
	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated Please use the corresponding type-specific method instead.
	 */
	@Deprecated
	@Override
	default Byte get(final Object key) {
		if (key == null)
			return null;
		final double k = ((Double) (key)).doubleValue();
		final byte v = get(k);
		return (v != defaultReturnValue() || containsKey(k)) ? Byte.valueOf(v) : null;
	}
	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated Please use the corresponding type-specific method instead.
	 */
	@Deprecated
	@Override
	default Byte remove(final Object key) {
		if (key == null)
			return null;
		final double k = ((Double) (key)).doubleValue();
		return containsKey(k) ? Byte.valueOf(remove(k)) : null;
	}
	/**
	 * Returns true if this function contains a mapping for the specified key.
	 *
	 * <p>
	 * Note that for some kind of functions (e.g., hashes) this method will always
	 * return true. In particular, this default implementation always returns true.
	 *
	 * @param key
	 *            the key.
	 * @return true if this function associates a value to {@code key}.
	 * @see Function#containsKey(Object)
	 */
	default boolean containsKey(double key) {
		return true;
	}
	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated Please use the corresponding type-specific method instead.
	 */
	@Deprecated
	@Override
	default boolean containsKey(final Object key) {
		return key == null ? false : containsKey(((Double) (key)).doubleValue());
	}
	/**
	 * Sets the default return value (optional operation).
	 *
	 * This value must be returned by type-specific versions of {@code get()},
	 * {@code put()} and {@code remove()} to denote that the map does not contain
	 * the specified key. It must be 0/{@code false}/{@code null} by default.
	 *
	 * @param rv
	 *            the new default return value.
	 * @see #defaultReturnValue()
	 */
	default void defaultReturnValue(byte rv) {
		throw new UnsupportedOperationException();
	}
	/**
	 * Gets the default return value.
	 *
	 * <p>
	 * This default implementation just return the default null value of the type
	 * ({@code null} for objects, 0 for scalars, false for Booleans).
	 *
	 * @return the current default return value.
	 */
	default byte defaultReturnValue() {
		return ((byte) 0);
	}
	default it.unimi.dsi.fastutil.doubles.Double2ByteFunction andThen(
			it.unimi.dsi.fastutil.bytes.Byte2ByteFunction after) {
		return k -> after.get(get(k));
	}
	default it.unimi.dsi.fastutil.bytes.Byte2ByteFunction compose(
			it.unimi.dsi.fastutil.bytes.Byte2DoubleFunction before) {
		return k -> get(before.get(k));
	}
	default it.unimi.dsi.fastutil.doubles.Double2ShortFunction andThen(
			it.unimi.dsi.fastutil.bytes.Byte2ShortFunction after) {
		return k -> after.get(get(k));
	}
	default it.unimi.dsi.fastutil.shorts.Short2ByteFunction compose(
			it.unimi.dsi.fastutil.shorts.Short2DoubleFunction before) {
		return k -> get(before.get(k));
	}
	default it.unimi.dsi.fastutil.doubles.Double2IntFunction andThen(
			it.unimi.dsi.fastutil.bytes.Byte2IntFunction after) {
		return k -> after.get(get(k));
	}
	default it.unimi.dsi.fastutil.ints.Int2ByteFunction compose(it.unimi.dsi.fastutil.ints.Int2DoubleFunction before) {
		return k -> get(before.get(k));
	}
	default it.unimi.dsi.fastutil.doubles.Double2LongFunction andThen(
			it.unimi.dsi.fastutil.bytes.Byte2LongFunction after) {
		return k -> after.get(get(k));
	}
	default it.unimi.dsi.fastutil.longs.Long2ByteFunction compose(
			it.unimi.dsi.fastutil.longs.Long2DoubleFunction before) {
		return k -> get(before.get(k));
	}
	default it.unimi.dsi.fastutil.doubles.Double2CharFunction andThen(
			it.unimi.dsi.fastutil.bytes.Byte2CharFunction after) {
		return k -> after.get(get(k));
	}
	default it.unimi.dsi.fastutil.chars.Char2ByteFunction compose(
			it.unimi.dsi.fastutil.chars.Char2DoubleFunction before) {
		return k -> get(before.get(k));
	}
	default it.unimi.dsi.fastutil.doubles.Double2FloatFunction andThen(
			it.unimi.dsi.fastutil.bytes.Byte2FloatFunction after) {
		return k -> after.get(get(k));
	}
	default it.unimi.dsi.fastutil.floats.Float2ByteFunction compose(
			it.unimi.dsi.fastutil.floats.Float2DoubleFunction before) {
		return k -> get(before.get(k));
	}
	default it.unimi.dsi.fastutil.doubles.Double2DoubleFunction andThen(
			it.unimi.dsi.fastutil.bytes.Byte2DoubleFunction after) {
		return k -> after.get(get(k));
	}
	default it.unimi.dsi.fastutil.doubles.Double2ByteFunction compose(
			it.unimi.dsi.fastutil.doubles.Double2DoubleFunction before) {
		return k -> get(before.get(k));
	}
	default <T> it.unimi.dsi.fastutil.doubles.Double2ObjectFunction<T> andThen(
			it.unimi.dsi.fastutil.bytes.Byte2ObjectFunction<T> after) {
		return k -> after.get(get(k));
	}
	default <T> it.unimi.dsi.fastutil.objects.Object2ByteFunction<T> compose(
			it.unimi.dsi.fastutil.objects.Object2DoubleFunction<T> before) {
		return k -> get(before.getDouble(k));
	}
	default <T> it.unimi.dsi.fastutil.doubles.Double2ReferenceFunction<T> andThen(
			it.unimi.dsi.fastutil.bytes.Byte2ReferenceFunction<T> after) {
		return k -> after.get(get(k));
	}
	default <T> it.unimi.dsi.fastutil.objects.Reference2ByteFunction<T> compose(
			it.unimi.dsi.fastutil.objects.Reference2DoubleFunction<T> before) {
		return k -> get(before.getDouble(k));
	}
}
