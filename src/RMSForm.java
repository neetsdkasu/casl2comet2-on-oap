import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException ;

public class RMSForm extends Form implements Compiler.Loader
{
	StringItem info;
	ChoiceGroup saveType;
	TextField saveName;
	ChoiceGroup fileList;
	RecordStore curRS = null;
	String fileName = null;
	boolean isNewFile = false;

	public RMSForm()
	{
		super("Files");

		info = new StringItem(null, null);
		append(info);

		saveType = new ChoiceGroup("save type:", ChoiceGroup.EXCLUSIVE);
		saveType.append("NEW", null);
		saveType.append("LOAD", null);
		saveType.setSelectedIndex(0, true);
		append(saveType);

		saveName = new TextField("new file:", "", 8, TextField.ANY);
		append(saveName);

		fileList = new ChoiceGroup("load files:", ChoiceGroup.EXCLUSIVE);
		String[] list = RecordStore.listRecordStores();
		if (list != null)
		{
			for (int i = 0; i < list.length; i++)
			{
				if (list[i].startsWith("casl2."))
				{
					fileList.append(list[i].substring(6), null);
				}
			}
		}
		append(fileList);
	}

	public void clearFields()
	{
		info.setText(null);
	}

	public void close()
	{
		if (curRS == null)
		{
			return;
		}
		try
		{
			curRS.closeRecordStore();
		}
		catch (RecordStoreException _)
		{
			// no code
		}
		curRS = null;
	}

	public String getFileName()
	{
		return fileName;
	}

	public boolean isValid()
	{
		if (saveType.getSelectedIndex() == 0) // NEW
		{
			String file = saveName.getString();
			if (file == null || file.length() == 0 || file.length() > 8)
			{
				info.setText("wrong file name (len=1-8)");
				return false;
			}
			file = file.toUpperCase();
			int head = file.charAt(0);
			if (head < 'A'|| 'Z' < head)
			{
				info.setText("wrong file name (head=A-Z)");
				return false;
			}
			for (int i = 0; i < file.length(); i++)
			{
				int ch = file.charAt(i);
				if ('A' <= ch && ch <= 'Z')
				{
					continue;
				}
				if ('0' <= ch && ch <= '9')
				{
					continue;
				}
				info.setText("wrong file name (char=A-Z0-9)");
				return false;
			}
			for (int i = 0; i < fileList.size(); i++)
			{
				if (file.equals(fileList.getString(i)))
				{
					info.setText("wrong file name (duplicate)");
					return false;
				}
			}
			fileName = file;
			isNewFile = true;
		}
		else
		{
			if (fileList.size() == 0)
			{
				info.setText("no file");
				return false;
			}
			int idx = fileList.getSelectedIndex();
			if (idx < 0)
			{
				info.setText("no selected");
				return false;
			}
			fileName = fileList.getString(idx);
			isNewFile = false;
		}
		info.setText(null);
		return true;
	}

	public boolean saveSrc(String src)
	{
		if (curRS == null)
		{
			return false;
		}
		if (src == null || src.length() == 0)
		{
			String rsName = "casl2." + fileName;
			try
			{
				curRS.closeRecordStore();
			}
			catch (RecordStoreException __)
			{
				// no code
			}
			curRS = null;
			try
			{
				RecordStore.deleteRecordStore(rsName);
			}
			catch (RecordStoreException __)
			{
				// no code
			}
			for (int i = 0; i < fileList.size(); i++)
			{
				if (fileName.equals(fileList.getString(i)))
				{
					fileList.delete(i);
					break;
				}
			}
			return false;
		}
		try
		{
			byte[] buf = src.getBytes();
			if (curRS.getNumRecords() == 0)
			{
				curRS.addRecord(buf, 0, buf.length);
			}
			else
			{
				curRS.setRecord(1, buf, 0, buf.length);
			}
			return true;
		}
		catch (RecordStoreException ex)
		{
			CASL2MIDlet.lastError = ex.toString();
			try
			{
				curRS.closeRecordStore();
			}
			catch (RecordStoreException __)
			{
				// no code
			}
			curRS = null;
			return false;
		}
	}

	public String getSrc()
	{
		if (fileName == null)
		{
			CASL2MIDlet.lastError = "fileName is null";
			return null;
		}
		String rsName = "casl2." + fileName;
		if (curRS != null)
		{
			try
			{
				if (rsName.equals(curRS.getName()) == false)
				{
					curRS.closeRecordStore();
					curRS = null;
				}
			}
			catch (RecordStoreException  ex)
			{
				CASL2MIDlet.lastError = ex.toString();
				if (curRS != null)
				try
				{
					curRS.closeRecordStore();
				}
				catch (RecordStoreException __)
				{
				}
				curRS = null;
				return null;
			}
		}
		try
		{
			if (curRS == null) {
				curRS = RecordStore.openRecordStore(rsName, true);
				if (isNewFile)
				{
					for (int i = 0; i < fileList.size(); i++)
					{
						if (fileName.equals(fileList.getString(i)))
						{
							isNewFile = false;
							break;
						}
					}
					if (isNewFile)
					{
						fileList.append(fileName, null);
						isNewFile = false;
					}
				}
			}
			if (curRS.getNumRecords() == 0)
			{
				return "";
			}
			byte[] buf = curRS.getRecord(1);
			if (buf == null)
			{
				return "";
			}
			return new String(buf);
		}
		catch (RecordStoreException ex)
		{
			CASL2MIDlet.lastError = ex.toString();
			if (curRS != null)
			{
				try
				{
					curRS.closeRecordStore();
				}
				catch (RecordStoreException __)
				{
					// no code
				}
				curRS = null;
			}
			return null;
		}

	}

	public String loadSrc(String name)
	{
		if (name == null)
		{
			CASL2MIDlet.lastError = "name is null";
			return null;
		}
		boolean found = false;
		for (int i = 0; i < fileList.size(); i++)
		{
			if (name.equals(fileList.getString(i)))
			{
				found = true;
				break;
			}
		}
		if (!found)
		{
			CASL2MIDlet.lastError = "name is not found";
			return null;
		}
		String rsName = "casl2." + name;
		RecordStore rs = null;
		if (curRS != null)
		{
			try
			{
				if (rsName.equals(curRS.getName()))
				{
					rs = curRS;
				}
			}
			catch (RecordStoreException  ex)
			{
				CASL2MIDlet.lastError = ex.toString();
				if (curRS != null)
				try
				{
					curRS.closeRecordStore();
				}
				catch (RecordStoreException __)
				{
					// no code
				}
				curRS = null;
				return null;
			}
		}
		try
		{
			if (rs == null) {
				rs = RecordStore.openRecordStore(rsName, false);
			}
			if (rs.getNumRecords() == 0)
			{
				return "";
			}
			byte[] buf = rs.getRecord(1);
			if (buf == null)
			{
				return "";
			}
			return new String(buf);
		}
		catch (RecordStoreException ex)
		{
			CASL2MIDlet.lastError = ex.toString();
			if (rs != null)
			{
				try
				{
					rs.closeRecordStore();
				}
				catch (RecordStoreException __)
				{
					// no code
				}
				if (rs == curRS)
				{
					curRS = null;
				}
			}
			return null;
		}
		finally
		{
			if (rs != null && rs != curRS)
			{
				try
				{
					rs.closeRecordStore();
				}
				catch (RecordStoreException __)
				{
					// no code
				}
			}
		}
	}

}