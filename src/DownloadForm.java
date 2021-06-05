import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Ticker;

public class DownloadForm extends Form implements Runnable
{
	Ticker info = null;
	TextField nameField, urlField;

	RMSForm rms;
	
	volatile int result = -1;

	void showInfo(String msg)
	{
		if (info == null)
		{
			info = new Ticker(msg);
		}
		else
		{
			info.setString(msg);
		}
		setTicker(info);
	}

	public DownloadForm(RMSForm rms)
	{
		super("download");

		this.rms = rms;

		nameField = new TextField("name:", "", 8, TextField.ANY);
		append(nameField);

		urlField = new TextField("url:", "", 500, TextField.ANY);
		append(urlField);
	}

	public String getName()
	{
		return nameField.getString();
	}

	public boolean isValid()
	{
		String name = nameField.getString();
		String url = urlField.getString();
		if (name == null || name.length() == 0)
		{
			showInfo("require name");
			return false;
		}
		if (url == null || url.length() == 0)
		{
			showInfo("require url");
			return false;
		}
		if (url.length() < 13)
		{
			showInfo("wrong url");
			return false;
		}
		String msg = rms.validateName(name);
		if (msg != null)
		{
			showInfo(msg);
			return false;
		}
		if (!url.startsWith("http://") && !url.startsWith("https://"))
		{
			showInfo("wrong url");
			return false;
		}

		setTicker(null);
		return true;
	}
	
	public int getResult()
	{
		return result;
	}
	
	public void run()
	{
		result = doDownload();
	}

	private int doDownload()
	{
		String name = nameField.getString();
		String url = urlField.getString();
		HttpConnection conn = null;
		InputStream is = null;
		ByteArrayOutputStream baos = null;
		byte[] data = null;
		try
		{
			conn = (HttpConnection)Connector.open(url);

			int rc = conn.getResponseCode();
			if (rc != HttpConnection.HTTP_OK)
			{
				String msg = conn.getResponseMessage();
				showInfo("failed: " + String.valueOf(msg));
				return -1;
			}

			is = conn.openInputStream();

			long lenL = conn.getLength();
			if (lenL > 5000L)
			{
				showInfo("too big file!");
				return -1;
			}
			int len = (int)lenL;

			if (len > 0) {
				int actual = 0;
				int bytesread = 0 ;
				data = new byte[len];
				while ((bytesread != len) && (actual != -1)) {
					actual = is.read(data, bytesread, len - bytesread);
					bytesread += actual;
				}
				if (actual < 0)
				{
					showInfo("unknown error");
					return -1;
				}
			} else {
				baos = new ByteArrayOutputStream(5001);
				int ch;
				while ((ch = is.read()) != -1) {
					baos.write(ch);
					if (baos.size() > 5000)
					{
						showInfo("too big file!");
						return -1;
					}
				}
				data = baos.toByteArray();
			}
		}
		catch (Exception ex)
		{
			CASL2MIDlet.lastError = ex.toString();
			showInfo("unknown error");
			return -1;
		}
		finally
		{
			if (is != null)
			{
				try { is.close(); }
				catch (Exception ex) {}
				is = null;
			}
			if (conn != null)
			{
				try { conn.close(); }
				catch (Exception ex) {}
				conn = null;
			}
			if (baos != null)
			{
				try { baos.close(); }
				catch (Exception ex) {}
				baos = null;
			}
		}
		return rms.saveSrc(name.toUpperCase(), data);
	}
}