public class AtomicInteger
{
	private volatile int value;
	public AtomicInteger(int initialValue)
	{
		value = initialValue;
	}
	public AtomicInteger()
	{
		this(0);
	}
	public int get()
	{
		int ret = 0;
		synchronized(this)
		{
			ret = value;
		}
		return ret;
	}
	public void set(int newValue)
	{
		int oldValue = get();
		while (compareAndSet(oldValue, newValue) == false)
		{
			oldValue = get();
		}
	}
	public boolean compareAndSet(int expect, int update)
	{
		synchronized(this)
		{
			if (value == expect)
			{
				value = update;
				return true;
			}
			else
			{
				return false;
			}
		}
	}
}