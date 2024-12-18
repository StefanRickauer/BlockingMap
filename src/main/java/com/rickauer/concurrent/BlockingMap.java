/**
 * A thread safe map-like data structure designed for handling asynchronous API responses.
 * Provides blocking retrieval, optional timeouts, and thread-safe updates.
 *
 * <p>This class is ideal for scenarios where multiple threads process shared data,
 * such as managing API calls and their responses. Built on {@link ConcurrentHashMap}
 * and {@link CompletableFuture}, it ensures high performance and simplicity.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Blocking get operations with optional timeout</li>
 *   <li>Thread-safe put and remove operations</li>
 *   <li>Support for cleanup of completed entries</li>
 * </ul>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */

package com.rickauer.concurrent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

public class BlockingMap<K, V> {
	
	private final ConcurrentHashMap<K, CompletableFuture<V>> map;
	
	
	/**
	 * Creates an empty BlockingMap with no pre-configured capacity or behavior.
	 */
	public BlockingMap() {
		map = new ConcurrentHashMap<>();
	}
	
	
	/**
	 * Creates a BlockingMap with an initial capacity.
	 *
	 * @param initialCapacity the initial capacity of the underlying map
	 * @throws IllegalArgumentException if the initial capacity is negative
	 */
	public BlockingMap(int initialCapacity) {
		map = new ConcurrentHashMap<>(initialCapacity);
	}
	
	
	/**
	 * Creates a BlockingMap pre-populated with the specified initial data.
	 * All entries in the provided map will be added to the BlockingMap.
	 *
	 * @param initialData the map whose entries are to be placed in this BlockingMap
	 * @throws NullPointerException if the initialData map is null or contains null keys/values
	 */
	public BlockingMap(Map<K, V> initialData) {
		map = new ConcurrentHashMap<>();
		initialData.forEach((key, value) -> map.put(key, CompletableFuture.completedFuture(value)));
	}
	
	
	/**
	 * Inserts a value into the map and signals any threads waiting on this key.
	 * If the key already exists, the value is replaced.
	 *
	 * @param key   the key with which the specified value is to be associated
	 * @param value the value to associate with the key
	 * @throws NullPointerException if the key or value is null
	 */ 
	public void put(K key, V value) {
		if (value == null) {
			throw new NullPointerException("Value cannot be null.");
		}
		
		if (isAvailable(key)) {
			remove(key);
		}

		map.computeIfAbsent(key, k -> new CompletableFuture<>()).complete(value);
	}
	
	
	/**
	 * Adds the specified value to the map only if the specified key is not already associated with a value.
	 * If the key is already present in the map, the existing value is retained, and the new value is ignored.
	 *
	 * @param key   the key with which the specified value is to be associated
	 * @param value the value to be added if the key is not already present
	 */
	public void putIfAbsent(K key, V value) {
		if (value == null) {
			throw new NullPointerException("Value cannot be null.");
		}
		map.computeIfAbsent(key, k -> new CompletableFuture<>()).complete(value);
	}
	
	
	/**
	 * Retrieves the value associated with the specified key. If no value is available, 
	 * the method blocks until a value is put into the map.
	 *
	 * @param key the key whose associated value is to be returned
	 * @return the value associated with the specified key
	 * @throws InterruptedException if the current thread is interrupted while waiting
	 * @throws ExecutionException   if the computation associated with the key fails 
	 */
	public V get(K key) throws InterruptedException, ExecutionException {
		return map.computeIfAbsent(key, k -> new CompletableFuture<>()).get();
	}
	
	
	/**
	 * Retrieves the value associated with the specified key, blocking until the value becomes available 
	 * or the specified timeout is reached.
	 *
	 * @param key     the key whose associated value is to be returned
	 * @param timeout the maximum time to wait for the value
	 * @param unit    the time unit of the timeout argument
	 * @return the value associated with the specified key
	 * @throws InterruptedException if the current thread is interrupted while waiting
	 * @throws ExecutionException   if the computation associated with the key fails
	 * @throws TimeoutException     if the timeout elapses before the value becomes available
	 */
	public V get(K key, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return map.computeIfAbsent(key, k -> new CompletableFuture<>()).get(timeout, unit);
	}
	
	
	/**
	 * Checks if the map is empty, meaning there are no pending or completed entries.
	 *
	 * @return true if the map contains no key-value pairs, false otherwise
	 */
	public boolean isEmpty() {
		return map.isEmpty();
	}
	
	
	/**
	 * Checks if the value for the specified key is already available (i.e., completed).
	 *
	 * @param key the key to check for availability
	 * @return true if the value for the key is available, false otherwise
	 */
	public boolean isAvailable(K key) {
		CompletableFuture<V> future = map.get(key);
		return future != null && future.isDone();
	}
	
	
	/**
	 * Iterates over all keys with completed values and applies the specified action.
	 * This method does not block on incomplete entries.
	 *
	 * @param action the action to be performed for each completed key-value pair
	 * @throws NullPointerException if the specified action is null
	 */
	public void forEachAvailable(BiConsumer<K, V> action) {
		map.forEach((key, future) -> {
			if (future.isDone()) {
				action.accept(key, future.join());
			}
		});
	}
	
	
	/**
	 * Removes the entry for the specified key from the map and returns its value 
	 * if it was completed. If the key is not present or the value is incomplete, 
	 * returns null.
	 *
	 * @param key the key whose mapping is to be removed from the map
	 * @return the previously associated value, or null if no such value exists or is incomplete
	 */
	public V remove(K key) {
		CompletableFuture<V> future = map.remove(key);
		return (future != null && future.isDone()) ? future.join() : null;
	}
	
	
	/**
	 * Removes all completed entries from the map. Useful for freeing up resources
	 * associated with processed entries.
	 */
	public void removeCompleted() {
		map.entrySet().removeIf(entry -> entry.getValue().isDone());
	}
	
	
	/**
	 * Returns the number of entries currently in the map, including both completed
	 * and pending entries.
	 *
	 * @return the number of entries in the map
	 */
	public int size() {
		return map.size();
	}
	
	
	/**
	 * Returns a set of all keys currently in the map. This includes keys with both
	 * completed and pending values.
	 *
	 * @return a set of all keys in the map
	 */
	public Set<K> keys() {
		return map.keySet();
	}
}
