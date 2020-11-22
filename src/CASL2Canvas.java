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
	private static final int STATE_STOP      = 4;
	private static final int STATE_INFO      = 5;
	
	private volatile boolean loop = true;
	private volatile boolean step = false;
	private final AtomicInteger state;
	volatile String srcCode = null;
	volatile String message = "";
	volatile int infoColor = 0xFFFFFF;
	Device keyBoard = null;
	
	public CASL2Canvas(Device kybd)
	{
		super(false);
		keyBoard = kybd;
		state = new AtomicInteger(STATE_IDLE);
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
		Console console = new Console(0xFFFFFF);
		comet2.setDevices(new Device[]{ keyBoard, console});
		Compiler compiler = new Compiler();
		loop = true;
		while (loop)
		{
			int nowState = state.get();
			switch (nowState)
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
				try
				{
					String res = compiler.compile(mem, srcCode);
					if (res == null)
					{
						comet2.resetRegisters();
						state.compareAndSet(nowState, STATE_RUNNING);
					}
					else
					{
						console.print(res, 0xFF0000);
						clear(g);
						console.paint(g);
						flushGraphics();
						state.compareAndSet(nowState, STATE_IDLE);
						// System.out.println(res);
					}
				}
				catch (Exception ex)
				{
					CASL2MIDlet.lastError = ex.toString();
					state.compareAndSet(nowState, STATE_IDLE);
				}
				break;
			case STATE_RUNNING:
				try
				{
					comet2.step();
					if (console.isUpdated() && !step)
					{
						clear(g);
						console.paint(g);
						flushGraphics();
					}
					if (step)
					{
						String[] status = comet2.getStatus();
						for (int i = 0; i < status.length; i++)
						{
							console.print(status[i], 0x00FFFF);
						}
						clear(g);
						console.paint(g);
						flushGraphics();
						state.compareAndSet(nowState, STATE_STEPWAIT);
					}
				}
				catch (CometIIError ex)
				{
					if (step)
					{
						String[] status = comet2.getStatus();
						for (int i = 0; i < status.length; i++)
						{
							console.print(status[i], 0x00FFFF);
						}
					}
					console.print(ex.toString(), 0xFF0000);
					console.print("Stoped", 0xFF0000);
					clear(g);
					console.paint(g);
					flushGraphics();
					state.compareAndSet(nowState, STATE_IDLE);
				}
				break;
			case STATE_STEPWAIT:
				{
					int keyStates = getKeyStates();
					if (keyStates == FIRE_PRESSED)
					{
						if (state.compareAndSet(nowState, STATE_RUNNING))
						{
							try
							{
								Thread.sleep(300);
								getKeyStates();
							}
							catch (InterruptedException _)
							{
								// no code
							}
						}
					}
				}
				break;
			case STATE_STOP:
				console.print("Stoped", 0xFF0000);
				clear(g);
				console.paint(g);
				flushGraphics();
				state.compareAndSet(nowState, STATE_IDLE);
				break;
			case STATE_INFO:
				console.print(message, infoColor);
				clear(g);
				console.paint(g);
				flushGraphics();
				state.compareAndSet(nowState, STATE_IDLE);
				break;
			}
		}
	}
	
	public void requestRun(String src, boolean step)
	{
		if (state.compareAndSet(STATE_IDLE, STATE_COMPILE))
		{
			srcCode = src;
			this.step = step;
		}
	}
	
	public void requestInfo(String msg, int ic)
	{
		if (state.compareAndSet(STATE_IDLE, STATE_INFO))
		{
			message = msg;
			infoColor = ic;
		}
	}

	public void requestStop()
	{
        int nowState = state.get();
		if (nowState != STATE_IDLE)
		{
			state.compareAndSet(nowState, STATE_STOP);
		}
	}
	
	public void requestEndLoop()
	{
		loop = false;
	}
}
