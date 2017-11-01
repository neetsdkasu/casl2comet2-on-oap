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
	
	public KeyBoard() {
		state = STATE_STANDBY;
		thread = null;
	}
	
	public void sendCommand(int cmd) {
		synchronized (lock) {
			if (thread != null) {
				return;
			}
			if (cmd == 1) {
				thread = new Thread(this);
				thread.start();
			} else {
				state = STATE_ERROR;
			}
		}
	}
	
	public void run()
	{
		try {
			for(;;) {
				if (state == STATE_PREPARE) {
					// data = System.in.read();
					if (data < 0 || data == LINEEND) {
						break;
					} else if (LINESEP.indexOf(data) < 0) {
						state = STATE_READY;
					}
				}
				Thread.sleep(1);
			}
			state = STATE_STANDBY;
		} catch (InterruptedException _) {
			state = STATE_ERROR;
		}
		thread = null;
	}
}
