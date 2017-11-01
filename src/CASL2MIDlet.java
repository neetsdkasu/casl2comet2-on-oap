// CASL2MIDlet

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

public final class CASL2MIDlet extends MIDlet implements CommandListener
{
	private Thread mainloop = null;
	
	private CASL2Canvas mainDisp  = null;
	private CodingBox   codingBox = null;
	
	private Command exitCommand     = null;
	private Command helpCommand     = null;
	private Command runCommand      = null;
	private Command stepCommand     = null;
	private Command codingCommand   = null;
	private Command closeCommand    = null;
	
	public CASL2MIDlet()
	{
		super();
		
		mainDisp = new CASL2Canvas();
		codingBox = new CodingBox();
		
		exitCommand = new Command("EXIT", Command.EXIT, 1);		
		mainDisp.addCommand(exitCommand);

		runCommand = new Command("RUN", Command.SCREEN, 1);
		mainDisp.addCommand(runCommand);
		
		stepCommand = new Command("STEP", Command.SCREEN, 2);
		mainDisp.addCommand(stepCommand);

		helpCommand = new Command("HELP", Command.SCREEN, 4);
		mainDisp.addCommand(helpCommand);
		
		codingCommand = new Command("CODING", Command.SCREEN, 3);
		mainDisp.addCommand(codingCommand);
		
		closeCommand = new Command("CLOSE", Command.SCREEN, 1);
		codingBox.addCommand(closeCommand);
		
		mainDisp.setCommandListener(this);
		codingBox.setCommandListener(this);
		
		Display.getDisplay(this).setCurrent(mainDisp);
	}
	
	private void requestEndLoop()
	{
		if (mainDisp != null)
		{
			mainDisp.requestEndLoop();
		}
		if (mainloop != null)
		{
			try
			{
				mainloop.join();
			}
			catch (InterruptedException _)
			{
				// no code
			}
		}		
	}
	
	protected void destroyApp(boolean unconditional)
			throws MIDletStateChangeException
	{
		requestEndLoop();
	}
	
	protected void pauseApp()
	{
		// no code
	}
	
	protected void startApp()
			throws MIDletStateChangeException
	{
		if (mainloop == null)
		{
			mainloop = new Thread(mainDisp);
			mainloop.start();
		}
	}
	
	public void commandAction(Command cmd, Displayable disp)
	{
		if (cmd == exitCommand)
		{
			requestEndLoop();
			notifyDestroyed();
		}
		else if (cmd == runCommand)
		{
			String src = codingBox.getString();
			mainDisp.requestRun(src, false);
		}
		else if (cmd == stepCommand)
		{
			String src = codingBox.getString();
			mainDisp.requestRun(src, true);
		}
		else if (cmd == codingCommand)
		{
			Display.getDisplay(this).setCurrent(codingBox);
		}
		else if (cmd == closeCommand)
		{
			Display.getDisplay(this).setCurrent(mainDisp);
		}
	}
}