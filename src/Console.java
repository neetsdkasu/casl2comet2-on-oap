import javax.microedition.lcdui.Graphics;

public class Console extends Device implements Runnable
{
	String[] texts = null;
	int curpos = 0;
	int lines = 0;
	
	final Object lock = new Object();
	volatile Thread thread = null;
	int buflen = 0;
	volatile boolean updated = false;
	
	public Console()
	{
		texts = new String[20];
	}
	
	public void print(String msg)
	{
		texts[curpos] = msg;
		curpos = (curpos + 1) % texts.length;
		if (lines < texts.length)
		{
			lines++;
		}
		updated = true;
	}
	
	public void paint(Graphics g)
	{
		int p = lines < texts.length ? 0 : curpos;
		for (int i = 0; i < lines; i++)
		{
			if (texts[p] != null)
			{
				g.drawString(texts[p], 0, i * 13, Graphics.LEFT | Graphics.TOP);
			}
			p = (p + 1) % texts.length;
		}
		updated = false;
	}
	
	public void sendCommand(int cmd)
	{
		synchronized (lock)
		{
			if (thread != null)
			{
				return;
			}
			if (cmd > 0)
			{
				buflen = cmd;
				thread = new Thread(this);
				thread.start();
			}
			else
			{
				updated = true;
			}
		}
	}

	public void run()
	{
		StringBuffer buf = new StringBuffer(buflen);
		int bsize = 0;
		state = STATE_READY;
		try {
			for(;;) {
				if (state == STATE_PREPARE) {
					buf.append((char)data);
					bsize++;
					if (bsize < buflen)
					{
						state = STATE_READY;
					}
					else
					{
						break;
					}
				}
				Thread.sleep(1);
			}
			state = STATE_STANDBY;
		} catch (InterruptedException _) {
			state = STATE_ERROR;
		}
		print(buf.toString());
		thread = null;
	}
}