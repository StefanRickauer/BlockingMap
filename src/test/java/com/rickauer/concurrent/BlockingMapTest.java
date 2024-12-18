package com.rickauer.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

class BlockingMapTest {

	@Test
	void defaultConstructorTest() {
		BlockingMap<String, String> map = new BlockingMap<>();
		assertTrue(map.isEmpty());
	}

	@Test
	void constructorWithInitialDataTest() throws InterruptedException, ExecutionException {
		Map<String, String> initialData = new HashMap<>();
		initialData.put("key1", "value1");
		initialData.put("key2", "value2");

		BlockingMap<String, String> map = new BlockingMap<>(initialData);

		assertEquals(2, map.size());
		assertEquals("value1", map.get("key1"), "Value for 'key1' should match the initial data.");
		assertEquals("value2", map.get("key2"), "Value for 'key2' should match the initial data.");
	}

	@SuppressWarnings("unused")
	@Test
	void constructorWithNullInitialData() {
		assertThrows(NullPointerException.class, () -> {
			new BlockingMap<>(null);
		}, "Constructor should throw NullPointerException when initial data is null.");
	}

	@SuppressWarnings("unused")
	@Test
	void constructorWithInvalidInitialDataTest() {
		Map<String, String> initialData = new HashMap<>();
		initialData.put(null, "value");

		assertThrows(NullPointerException.class, () -> {
			new BlockingMap<>(initialData);
		}, "Constructor should throw NullPointerException when initial Data contains null keys or values.");
	}

	@Test
	void constructorWithEmptyInitialData() {
		Map<String, String> initialData = new HashMap<>();

		BlockingMap<String, String> map = new BlockingMap<>(initialData);

		assertTrue(map.isEmpty(), "BlockingMap should be empty when initialized with an empty map.");
	}

	@Test
	void constructorWithInitialCapacityTest() {
		BlockingMap<String, String> map = new BlockingMap<>(16);

		assertTrue(map.isEmpty(), "BlockingMap should be empty after initialization with initial capacity.");
	}

	@SuppressWarnings("unused")
	@Test
	void constructorWithInvalidInitialCapacityTest() {
		assertThrows(IllegalArgumentException.class, () -> {
			new BlockingMap<>(-1);
		}, "Constructor should throw IllegalArgumentException for negative initial capacity.");
	}

	@Test
	void putAndGetTest() throws InterruptedException, ExecutionException {
		BlockingMap<String, String> map = new BlockingMap<>();
		map.put("key1", "value1");

		assertEquals("value1", map.get("key1"), "The value retrieved by get should match the value inserted by put.");
	}

	@Test
	void putOverwriteValueTest() throws InterruptedException, ExecutionException {
		BlockingMap<String, String> map = new BlockingMap<>();
		map.put("key1", "value1");
		map.put("key1", "value2");

		assertEquals("value2", map.get("key1"),
				"The last value inserted should overwrite the previous value for the same key.");
	}

	@Test
	void putIfAbsentAndGetTest() throws InterruptedException, ExecutionException {
		BlockingMap<String, String> map = new BlockingMap<>();
		map.putIfAbsent("key1", "value1");

		assertEquals("value1", map.get("key1"),
				"The value retrieved by get should match the value inserted by putIfAbsent.");
	}

	@Test
	void putIfAbsentIgnoreValueAndGetTest() throws InterruptedException, ExecutionException {
		BlockingMap<String, String> map = new BlockingMap<>();
		map.put("key1", "value1");
		map.putIfAbsent("key1", "value2");

		assertEquals("value1", map.get("key1"), "putIfAbsent should ignore the new value for the same key.");
	}

	@Test
	void putIfAbsentThrowsExeptionTest() {
		BlockingMap<String, String> map = new BlockingMap<>();
		
		assertThrows(NullPointerException.class, () -> {
			map.putIfAbsent("key1", null);
		}, "putIfAbsent should throw NullPointerException if null value is inserted.");
	}
	
	@Test
	void getAfterMultiplePutsTest() throws InterruptedException, ExecutionException {
		BlockingMap<String, String> map = new BlockingMap<>();

		Thread putThread = new Thread(() -> {
			try {
				Thread.sleep(50);
				map.put("key1", "value1");

				Thread.sleep(50);
				map.put("key1", "value2");
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});

		putThread.start();

		Thread.sleep(200); // make sure there is enough time for the second key to be inserted

		assertEquals("value2", map.get("key1"), "get should return the most recently put value after blocking.");
	}

	@Test
	void putWithNullKeyTest() {
		BlockingMap<String, String> map = new BlockingMap<>();

		assertThrows(NullPointerException.class, () -> {
			map.put("key1", null);
		}, "put should throw NullPointerException for null values");
	}

	@Test
	void getBlocksForMissingKeyTest() {
		BlockingMap<String, String> map = new BlockingMap<>();

		Thread getThread = new Thread(() -> {
			try {
				map.get("key1"); // should block until value is inserted
			} catch (InterruptedException | ExecutionException e) {
				Thread.currentThread().interrupt();
			}
		});

		getThread.start();

		try {
			Thread.sleep(100);
			assertTrue(getThread.isAlive(), "get should block when the key is missing.");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		getThread.interrupt(); // stop test thread
	}

	@Test
	void getWithTimeoutValuePresentTest() throws InterruptedException, ExecutionException, TimeoutException {
		BlockingMap<String, String> map = new BlockingMap<>();
		map.put("key", "value");

		String result = map.get("key", 1, TimeUnit.SECONDS);
		assertEquals("value", result, "The value retrieved by get should match the value inserted by put.");
	}

	@Test
	void getWithTimeoutValueNotPresentTest() {
		BlockingMap<String, String> map = new BlockingMap<>();

		assertThrows(TimeoutException.class, () -> {
			map.get("key", 500, TimeUnit.MILLISECONDS);
		}, "Expected TimeoutException when key is not present and timeout is reached.");
	}

	@Test
	void getWithTimeoutValueAddedDuringWaitTest() throws InterruptedException, ExecutionException, TimeoutException {
		BlockingMap<String, String> map = new BlockingMap<>();

		new Thread(() -> {
			try {
				Thread.sleep(200);
				map.put("key", "value");
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}).start();

		String result = map.get("key", 1, TimeUnit.SECONDS);
		assertEquals("value", result, "Expected value to be returned wehn added during wait period.");
	}

	@Test
	void getWithTimeoutNullKeyTest() {
		BlockingMap<String, String> map = new BlockingMap<>();

		assertThrows(NullPointerException.class, () -> {
			map.get(null, 1, TimeUnit.SECONDS);
		}, "Expected NullPointerException when null key is provided.");
	}

	@Test
	void getWithoutValueAndZeroTimeoutTest() {
		BlockingMap<String, String> map = new BlockingMap<>();

		assertThrows(TimeoutException.class, () -> {
			map.get("key", 0, TimeUnit.SECONDS);
		}, "Expected TimeoutException when timeout is 0 and key is not present.");
	}

	@Test
	void getWithoutValueAndNegativeTimeoutTest() {
		BlockingMap<String, String> map = new BlockingMap<>();

		assertThrows(TimeoutException.class, () -> {
			map.get("key", -1, TimeUnit.SECONDS);
		}, "Expected TimeoutException when timeout is negative and key is not present.");
	}

	@Test
	void getWithValueAndZeroTimeoutTest() throws InterruptedException, ExecutionException, TimeoutException {
		BlockingMap<String, String> map = new BlockingMap<>();
		map.put("key", "value");

		String result = map.get("key", 0, TimeUnit.SECONDS);
		assertEquals("value", result, "Expected value to be returned immediately when timeout is 0.");
	}

	@Test
	void getWithValueAndNegativeTimeoutTest() throws InterruptedException, ExecutionException, TimeoutException {
		BlockingMap<String, String> map = new BlockingMap<>();
		map.put("key", "value");

		String result = map.get("key", -1, TimeUnit.SECONDS);
		assertEquals("value", result, "Expected value to be returned immediately when timeout is negative.");
	}

	@Test
	void forEachAvailableNormalTest() {
		BlockingMap<String, String> map = new BlockingMap<>();
		map.put("key1", "value1");
		map.put("key2", "value2");

		StringBuilder result = new StringBuilder();
		map.forEachAvailable((key, value) -> result.append(key).append("=").append(value).append(";"));

		assertEquals("key1=value1;key2=value2;", result.toString(),
				"Expected all key-value pairs to be processed by forEachAvailable.");
	}

	@Test
	void forEachAvailableEmptyMapTest() {
		BlockingMap<String, String> map = new BlockingMap<>();

		int[] counter = { 0 };
		map.forEachAvailable((key, value) -> counter[0]++);

		assertEquals(0, counter[0], "Expected no action to be performed.");
	}

	@Test
	void forEachAvailableNullActionTest() {
		BlockingMap<String, String> map = new BlockingMap<>();
		map.put("key1", "value1");

		assertThrows(NullPointerException.class, () -> {
			map.forEachAvailable(null);
		}, "Expected NullPointerException when action is null");
	}

	@Test
	void forEachAvailableWithDelayedValuesTest() throws InterruptedException {
		BlockingMap<String, String> map = new BlockingMap<>();
		map.put("key1", "value1");
		map.put("key2", "value2");

		// delay insertion of key3
		new Thread(() -> {
			try {
				Thread.sleep(500);
				map.put("key3", "delayedValue");
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}).start();

		StringBuilder result = new StringBuilder();

		map.forEachAvailable((key, value) -> {
			result.append(key).append("=").append(value).append(";");
		});
		assertEquals("key1=value1;key2=value2;", result.toString(),
				"Expected only immediately available key-value pairs to be processed.");

		Thread.sleep(600);

		result.setLength(0);
		map.forEachAvailable((key, value) -> {
			result.append(key).append("=").append(value).append(";");
		});
		assertEquals("key1=value1;key2=value2;key3=delayedValue;", result.toString(),
				"Expected all key-value pairs to be processed after the delayed value is added.");
	}

	@Test
	void removeCompletedRemovesCompletedEntries() {
		BlockingMap<String, String> map = new BlockingMap<>();
		map.put("key1", "value1");
		map.put("key2", "value2");

		assertEquals(2, map.size(), "BlockingMap should contain 2 entries before removeCompleted.");

		map.removeCompleted();
		assertTrue(map.isEmpty(), "BlockingMap should be empty after removeCompleted.");
	}

	@Test
	void removeCompletedDoesNotRemovePendingEntries() throws InterruptedException, ExecutionException {
		BlockingMap<String, String> map = new BlockingMap<>();
		map.put("key1", "value1");
		map.put("key2", "value2");

		new Thread(() -> {
			try {
				Thread.sleep(100);
				map.put("key3", "value3");
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}).start();

		assertEquals(2, map.size());

		map.removeCompleted();
		assertTrue(map.isEmpty(),
				"BlockingMap should be empty after removeCompleted since key3 has not been added, yet.");

		Thread.sleep(600);
		assertEquals(1, map.size(),
				"BlockingMap should contain one value since it has been added after removeCompleted.");
		assertEquals("value3", map.get("key3"));
	}

	@Test
	void keysReturnsAllKeysTest() {
		BlockingMap<String, String> map = new BlockingMap<>();

		map.put("key1", "value1");
		map.put("key2", "value2");
		map.put("key3", "value3");

		Set<String> keys = map.keys();

		assertTrue(keys.contains("key1"), "keys should contain key1.");
		assertTrue(keys.contains("key2"), "keys should contain key2.");
		assertTrue(keys.contains("key3"), "keys should contain key3.");
	}

	@Test
	void keysReturnsEmptySetForEmptyMap() {
		BlockingMap<String, String> map = new BlockingMap<>();

		Set<String> keys = map.keys();

		assertTrue(keys.isEmpty(), "keys should be empty for an empty map.");
	}

	@Test
	void removeRemovesExistingEntry() {
		BlockingMap<String, String> map = new BlockingMap<>();

		map.put("key1", "value1");

		assertTrue(map.isAvailable("key1"), "key1 should exist before remove.");

		map.remove("key1");

		assertFalse(map.isAvailable("key1"), "key1 should not exist after remove.");
	}

	@Test
	void removeDoesNotRemoveNonExistingEntry() {
		BlockingMap<String, String> map = new BlockingMap<>();

		String result = map.remove("nonExistingKey");

		assertNull(result, "Removing a non-existing key should return null.");
	}
}
