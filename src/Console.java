// Console

import javax.microedition.lcdui.Graphics;

public class Console extends Device implements Runnable
{
	String[] texts = null;
	int[] colors = null;
	int curpos = 0;
	int lines = 0;
	
	final Object lock = new Object();
	volatile Thread thread = null;
	volatile int buflen = 0;
	volatile boolean updated = false;
	int defaultColor = 0xFFFFFF;
	
	public Console(int dc)
	{
		texts = new String[20];
		colors = new int[texts.length];
		defaultColor = dc;
	}
	
	public boolean isUpdated()
	{
		return updated;
	}	
	
	public void print(String msg)
	{
		print(msg, defaultColor);
	}
	
	public void print(String msg, int col)
	{
		texts[curpos] = msg;
		colors[curpos] = col;
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
				g.setColor(colors[p]);
				g.drawString(texts[p], 0, i * 13, Graphics.LEFT | Graphics.TOP);
			}
			p = (p + 1) % texts.length;
		}
		updated = false;
	}
	
	public void putBuffer(String buf)
	{
		print(buf);
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
				state = STATE_READY;
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
				int st = state;
				if (st == STATE_PREPARE) {
					if (bsize >= buflen)
					{
						break;
					}
					buf.append((char)data);
					bsize++;
					state = STATE_READY;
				}
				Thread.sleep(1);
			}
			state = STATE_STANDBY;
		} catch (InterruptedException _) {
			state = STATE_ERROR;
		}
		// System.out.println("buflen: " + buflen);
		// System.out.println("reallen: " + buf.length());
		print(buf.toString());
		thread = null;
	}
}