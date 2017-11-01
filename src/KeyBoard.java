// KeyBoard

public class KeyBoard extends Device implements Runnable
{
	static final String LINESEP;
	static final char LINEEND;
	static {
		LINESEP = "\n";
		LINEEND = LINESEP.charAt(LINESEP.length() - 1);
	}
	
	final Object lock = new Object();
	volatile Thread thread;
	Caller caller = null;
	volatile String buffer = null;
	
	public KeyBoard(Caller clr) {
		state = STATE_STANDBY;
		thread = null;
		caller = clr;
	}
	
	public void setInput(String buf)
	{
		buffer = buf;
	}
	
	public void sendCommand(int cmd) {
		synchronized (lock) {
			if (thread != null) {
				return;
			}
			buffer = null;
			if (cmd == 1) {
				thread = new Thread(this);
				thread.start();
			} else {
				state = STATE_ERROR;
			}
		}
	}
	
	public String getBuffer()
	{
		return buffer;
	}
	
	public void run()
	{
		buffer = null;
		caller.call();
		while (buffer == null)
		{
			try
			{
				Thread.sleep(300);
			}
			catch (InterruptedException _)
			{
				break;
			}
		}
		/*
		try {
			int pos = 0;
			for(;;) {
				if (state == STATE_PREPARE) {
					if (pos >= buffer.length())
					{
						break;
					}
					else
					{
						data = buffer.charAt(pos);
						pos++;
						state = STATE_READY;
					}
				}
				Thread.sleep(1);
			}
			state = STATE_STANDBY;
		} catch (InterruptedException _) {
			state = STATE_ERROR;
		}
		buffer = null;
		*/
		thread = null;
	}
}
