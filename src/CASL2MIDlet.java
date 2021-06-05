// CASL2MIDlet

import java.io.InputStream;
import java.io.InputStreamReader;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

public final class CASL2MIDlet extends MIDlet implements CommandListener, Caller
{
	public static String lastError = "nothing";

	private Thread mainloop = null, downloading = null;
	
	private Alert waitDownload = null;

	private CASL2Canvas mainDisp  = null;
	private CodingBox   codingBox = null;
	private RMSForm     fileMgr   = null;
	private InputBox    inputBox  = null;
	private Form        helpView  = null;
	private DownloadForm download = null;

	private Command exitCommand     = null;
	private Command helpCommand     = null;
	private Command runCommand      = null;
	private Command stepCommand     = null;
	private Command stopCommand     = null;
	private Command fileCommand     = null;
	private Command codingCommand   = null;
	private Command closeCommand    = null;
	private Command cancelCommand   = null;
	private Command selectCommand   = null;
	private Command okCommand       = null;
	private Command breakCommand    = null;
	private Command errorCommand    = null;
	private Command closeHelpCommand= null;
	private Command httpCommand       = null;
	private Command httpGetCommand    = null;
	private Command httpCancelCommand = null;

	private boolean existFile = false;

	private KeyBoard keyBoard = null;

	public CASL2MIDlet()
	{
		super();

		keyBoard = new KeyBoard(this);

		mainDisp  = new CASL2Canvas(keyBoard);
		codingBox = new CodingBox();
		fileMgr   = new RMSForm();
		inputBox  = new InputBox();
		helpView  = new Form("Help");
		download = new DownloadForm(fileMgr);

		mainDisp.setLoader(fileMgr);

		exitCommand = new Command("EXIT", Command.EXIT, 1);
		mainDisp.addCommand(exitCommand);

		runCommand = new Command("RUN", Command.SCREEN, 1);
		mainDisp.addCommand(runCommand);

		stepCommand = new Command("STEP", Command.SCREEN, 2);
		mainDisp.addCommand(stepCommand);

		stopCommand = new Command("STOP", Command.SCREEN, 3);
		mainDisp.addCommand(stopCommand);

		fileCommand = new Command("FILE", Command.SCREEN, 4);
		mainDisp.addCommand(fileCommand);

		codingCommand = new Command("CODING", Command.SCREEN, 5);
		mainDisp.addCommand(codingCommand);

		httpCommand = new Command("HTTP", Command.SCREEN, 6);
		mainDisp.addCommand(httpCommand);

		errorCommand = new Command("ERROR", Command.SCREEN, 7);
		mainDisp.addCommand(errorCommand);

		helpCommand = new Command("HELP", Command.SCREEN, 8);
		mainDisp.addCommand(helpCommand);

		closeCommand = new Command("CLOSE", Command.SCREEN, 1);
		codingBox.addCommand(closeCommand);

		cancelCommand = new Command("CANCEL", Command.SCREEN, 1);
		fileMgr.addCommand(cancelCommand);

		selectCommand = new Command("SELECT", Command.SCREEN, 2);
		fileMgr.addCommand(selectCommand);

		okCommand = new Command("OK", Command.SCREEN, 1);
		inputBox.addCommand(okCommand);

		breakCommand = new Command("BREAK", Command.SCREEN, 2);
		inputBox.addCommand(breakCommand);

		closeHelpCommand = new Command("CLOSE", Command.SCREEN, 1);
		helpView.addCommand(closeHelpCommand);

		httpGetCommand = new Command("GET", Command.OK, 1);
		download.addCommand(httpGetCommand);

		httpCancelCommand = new Command("CANCEL", Command.CANCEL, 2);
		download.addCommand(httpCancelCommand);

		mainDisp.setCommandListener(this);
		codingBox.setCommandListener(this);
		fileMgr.setCommandListener(this);
		inputBox.setCommandListener(this);
		helpView.setCommandListener(this);
		download.setCommandListener(this);

		loadHelpText();

		Display.getDisplay(this).setCurrent(mainDisp);
	}

	public void call()
	{
		Display.getDisplay(this).setCurrent(inputBox);
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
		if (fileMgr != null)
		{
			if (existFile && codingBox != null)
			{
				String src = codingBox.getString();
				fileMgr.saveSrc(src);
			}
			fileMgr.close();
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
			if (existFile)
			{
				String src = codingBox.getString();
				String file = fileMgr.getFileName();
				mainDisp.requestRun(file, src, false);
			}
			else
			{
				mainDisp.requestInfo("must select file", 0xFF00FF);
			}
		}
		else if (cmd == stepCommand)
		{
			if (existFile)
			{
				String src = codingBox.getString();
				String file = fileMgr.getFileName();
				mainDisp.requestRun(file, src, true);
			}
			else
			{
				mainDisp.requestInfo("select file", 0xFF00FF);
			}
		}
		else if (cmd == stopCommand)
		{
			mainDisp.requestStop();
		}
		else if (cmd == fileCommand)
		{
			Display.getDisplay(this).setCurrent(fileMgr);
		}
		else if (cmd == codingCommand)
		{
			if (existFile)
			{
				Display.getDisplay(this).setCurrent(codingBox);
			}
			else
			{
				mainDisp.requestInfo("select file", 0xFF00FF);
			}
		}
		else if (cmd == closeCommand)
		{
			String src = codingBox.getString();
			int sizeAvailable = fileMgr.saveSrc(src);
			existFile = sizeAvailable >= 0;
			mainDisp.requestInfo("size available: " + sizeAvailable + " bytes", 0xFFFF00);
			Display.getDisplay(this).setCurrent(mainDisp);
		}
		else if (cmd == cancelCommand)
		{
			Display.getDisplay(this).setCurrent(mainDisp);
		}
		else if (cmd == selectCommand)
		{
			if (!fileMgr.isValid())
			{
				return;
			}
			if (fileMgr.selectedDelete())
			{
				fileMgr.doDelete();
				return;
			}
			String src = fileMgr.getSrc();
			String file = fileMgr.getFileName();
			if (src == null)
			{
				existFile = false;
				mainDisp.requestInfo("failure load file: " + file, 0xFF0000);
			}
			else
			{
				existFile = true;
				codingBox.setString(src);
				mainDisp.requestInfo("success load file: " + file, 0xFFFF00);
			}
			Display.getDisplay(this).setCurrent(mainDisp);
		}
		else if (cmd == okCommand)
		{
			String value = inputBox.getString();
			keyBoard.setInput(value);
			Display.getDisplay(this).setCurrent(mainDisp);
		}
		else if (cmd == breakCommand)
		{
			String value = inputBox.getString();
			keyBoard.setInput(value);
			Display.getDisplay(this).setCurrent(mainDisp);
			mainDisp.requestStop();
		}
		else if (cmd == errorCommand)
		{
			Alert alert = new Alert("LastError", lastError, null, AlertType.INFO);
			alert.setTimeout(8000);
			Display.getDisplay(this).setCurrent(alert);
		}
		else if (cmd == helpCommand)
		{
			Display.getDisplay(this).setCurrent(helpView);
		}
		else if (cmd == closeHelpCommand)
		{
			Display.getDisplay(this).setCurrent(mainDisp);
		}
		else if (cmd == httpCommand)
		{
			Display.getDisplay(this).setCurrent(download);
		}
		else if (cmd == httpCancelCommand)
		{
			Display.getDisplay(this).setCurrent(mainDisp);
		}
		else if (cmd == httpGetCommand)
		{
			if (!download.isValid())
			{
				return;
			}
			downloading = new Thread(download);
			downloading.start();
			waitDownload = new Alert("download", "downloading...", null, null);
			waitDownload.setTimeout(Alert.FOREVER);
			Display.getDisplay(this).setCurrent(waitDownload);
			(new Thread(new Runnable(){
				public void run() {
					if (downloading != null)
					{
						try { downloading.join(); }
						catch (Exception ex) {}
					}
					commandAction(Alert.DISMISS_COMMAND, waitDownload);
				}
			})).start();
		}
		else if (cmd == Alert.DISMISS_COMMAND)
		{
			if (disp != null && disp == waitDownload)
			{
				if (downloading != null)
				{
					if (downloading.isAlive())
					{
						return;
					}
					downloading = null;
				}
				waitDownload = null;
				String file = download.getName();
				int result = download.getResult();
				if (result >= 0)
				{
					mainDisp.requestInfo("success download file: " + file, 0xFFFF00);
					try { Thread.sleep(100L); }
					catch (Exception ex) {}
					mainDisp.requestInfo("size available: " + result + " bytes", 0xFFFF00);
				}
				else
				{
					mainDisp.requestInfo("failure download file: " + file, 0xFF0000);
				}
				Display.getDisplay(this).setCurrent(mainDisp);
			}
		}
	}

	private boolean loadHelpText()
	{
		InputStream is = getClass().getResourceAsStream("/help.txt");
		if (is == null)
		{
			lastError = "not found resource";
			return false;
		}
		try
		{
			InputStreamReader isr = new InputStreamReader(is, "UTF-8");
			StringBuffer sb = new StringBuffer();
			for (;;)
			{
				int ch = isr.read();
				if (ch < 0)
				{
					break;
				}
				sb.append((char)ch);
			}
			helpView.append(sb.toString());
			return true;
		}
		catch (Exception ex)
		{
			lastError = ex.toString();
			return false;
		}
		finally
		{
			try
			{
				is.close();
			}
			catch (Exception _)
			{
				// no code
			}
		}
	}
}