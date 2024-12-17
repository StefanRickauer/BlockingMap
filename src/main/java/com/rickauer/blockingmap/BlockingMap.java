package com.rickauer.blockingmap;

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
	
	public BlockingMap() {
		map = new ConcurrentHashMap<>();
	}
	
	public BlockingMap(int initialCapacity) {
		map = new ConcurrentHashMap<>(initialCapacity);
	}
	
	public BlockingMap(Map<K, V> initialData) {
		map = new ConcurrentHashMap<>();
		initialData.forEach((key, value) -> map.put(key, CompletableFuture.completedFuture(value)));
	}
	
	// insert value 
	public void put(K key, V value) {
		map.computeIfAbsent(key, k -> new CompletableFuture<>()).complete(value);
	}
	
	// force put
	public void forcePut(K key, V value) {
		CompletableFuture<V> future = new CompletableFuture<>();
		future.complete(value);
		map.put(key, future);
	}
	
	// Thread safe and blocking value retrieval (will block until value is available)
	public V get(K key) throws InterruptedException, ExecutionException {
		return map.computeIfAbsent(key, k -> new CompletableFuture<>()).get();
	}
	
	// Thread safe and blocking value retrieval (will block until value is available or until timeout)
	public V get(K key, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return map.computeIfAbsent(key, k -> new CompletableFuture<>()).get(timeout, unit);
	}
	
	// Check if blocking map is empty
	public boolean isEmpty() {
		return map.isEmpty();
	}
	
	// Check if key is in map
	public boolean isAvailable(K key) {
		CompletableFuture<V> future = map.get(key);
		return future != null && future.isDone();
	}
	
	// iterate over blocking map
	public void forEachAvailable(BiConsumer<K, V> action) {
		map.forEach((key, future) -> {
			if (future.isDone()) {
				action.accept(key, future.join());
			}
		});
	}
	
	// Delete value
	public V remove(K key) {
		CompletableFuture<V> future = map.remove(key);
		return (future != null && future.isDone()) ? future.join() : null;
	}
	
	// remove completed futures from map
	public void removeCompleted() {
		map.entrySet().removeIf(entry -> entry.getValue().isDone());
	}
	
	// get size
	public int size() {
		return map.size();
	}
	
	// list all keys
	public Set<K> keys() {
		return map.keySet();
	}
}
