package meds.util;

public class KeyValuePair<K, V>
{
	private K key;
	private V value;

	public KeyValuePair(K key, V value)
	{
		this.key = key;
		this.value = value;
	}

	public K getKey()
	{
		return this.key;
	}

	public V getValue()
	{
		return this.value;
	}
}
