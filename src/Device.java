// Device

public abstract class Device
{
	public static final int STATE_ERROR = -1;
	public static final int STATE_STANDBY = 0;
	public static final int STATE_PREPARE = 1;
	public static final int STATE_READY = 2;
	
	protected volatile int state;
	protected volatile int data;
	
	public int getState()
	{
		return state;
	}
	
	public void setState(int state)
	{
		this.state = state;
	}
	
	public void putData(int data)
	{
		this.data = data;
	}
	
	public int getData()
	{
		return data;
	}
	
	public String getBuffer()
	{
		return null;
	}

	public void putBuffer(String buf)
	{
		// no code
	}

	public abstract void sendCommand(int cmd);
}
