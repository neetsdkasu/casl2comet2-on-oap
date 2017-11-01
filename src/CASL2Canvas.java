// CASL2Canvas

import java.util.Date;
import java.util.Random;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;

public final class CASL2Canvas extends GameCanvas implements Runnable
{
	private static final int STATE_IDLE      = 0;
	private static final int STATE_COMPILE   = 1;
	private static final int STATE_RUNNING   = 2;
	private static final int STATE_STEPWAIT  = 3;
	
	private volatile boolean loop = true;
	private volatile boolean step = false;
	private volatile int state = STATE_IDLE;
	volatile String srcCode = null;
	
	public CASL2Canvas()
	{
		super(false);
	}
	
	private void clear(Graphics g)
	{
		g.setColor(0x000000);
		g.fillRect(0, 0, 240, 268);
	}
	
	public void run()
	{
		Graphics g = getGraphics();
		{
			Font font = Font.getFont(Font.FACE_SYSTEM,Font.STYLE_PLAIN,Font.SIZE_SMALL);
			g.setFont(font);
		}
		clear(g);
		flushGraphics();
		CometII comet2 = new CometII(null);
		Memory mem = comet2.memory;
		Console console = new Console();
		Compiler compiler = new Compiler();
		loop = true;
		state = STATE_IDLE;
		while (loop)
		{
			switch (state)
			{
			case STATE_IDLE:
				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException _)
				{
					// no code
				}
				break;
			case STATE_COMPILE:
				{
					String res = compiler.compile(mem, srcCode);
					if (res == null)
					{
						comet2.resetRegisters();
						state = STATE_RUNNING;
					}
					else
					{
						console.print(res);
						clear(g);
						g.setColor(0xFF0000);
						console.paint(g);
						flushGraphics();
						state = STATE_IDLE;
						System.out.println(res);
					}
				}
				break;
			case STATE_RUNNING:
				try
				{
					comet2.step();
					if (step)
					{
						String[] status = comet2.getStatus();
						for (int i = 0; i < status.length; i++)
						{
							console.print(status[i]);
						}
						clear(g);
						g.setColor(0x00FFFF);
						console.paint(g);
						flushGraphics();
						state = STATE_STEPWAIT;
					}
				}
				catch (CometIIError ex)
				{
					console.print(ex.toString());
					clear(g);
					g.setColor(0xFF0000);
					console.paint(g);
					flushGraphics();
					state = STATE_IDLE;
				}
				break;
			case STATE_STEPWAIT:
				{
					int keyStates = getKeyStates();
					if (keyStates == FIRE_PRESSED)
					{
						state = STATE_RUNNING;
					}
				}
				break;
			}
		}
	}
	
	public void requestRun(String src, boolean step)
	{
		if (state == STATE_IDLE)
		{
			srcCode = src;
			this.step = step;
			state = STATE_COMPILE;
		}
	}

	
	public void requestEndLoop()
	{
		loop = false;
	}
}
