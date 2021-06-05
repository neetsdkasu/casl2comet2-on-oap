import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Ticker;
import javax.microedition.lcdui.TextField;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException ;

public class RMSForm extends Form implements Compiler.Loader
{
	Ticker info = null;
	ChoiceGroup saveType;
	TextField saveName, deleteName;
	ChoiceGroup fileList;
	RecordStore curRS = null;
	String fileName = null;
	boolean isNewFile = false;
	
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

	public RMSForm()
	{
		super("Files");

		saveType = new ChoiceGroup("save type:", ChoiceGroup.EXCLUSIVE);
		saveType.append("NEW", null);
		saveType.append("LOAD", null);
		saveType.append("DELETE", null);
		saveType.setSelectedIndex(0, true);
		append(saveType);

		saveName = new TextField("new file:", "", 8, TextField.ANY);
		append(saveName);

		deleteName = new TextField("delete file:", "", 8, TextField.ANY);
		append(deleteName);

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
		setTicker(null);
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
	
	public String validateName(String file)
	{
		if (file == null || file.length() == 0 || file.length() > 8)
		{
			return "wrong file name (len=1-8)";
		}
		file = file.toUpperCase();
		int head = file.charAt(0);
		if (head < 'A'|| 'Z' < head)
		{
			return "wrong file name (head=A-Z)";
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
			return "wrong file name (char=A-Z0-9)";
		}
		for (int i = 0; i < fileList.size(); i++)
		{
			if (file.equals(fileList.getString(i)))
			{
				return "wrong file name (already exists)";
			}
		}
		return null;
	}

	public boolean isValid()
	{
		if (saveType.getSelectedIndex() == 0) // NEW
		{
			String file = saveName.getString();
			if (file == null || file.length() == 0 || file.length() > 8)
			{
				showInfo("wrong file name (len=1-8)");
				return false;
			}
			file = file.toUpperCase();
			int head = file.charAt(0);
			if (head < 'A'|| 'Z' < head)
			{
				showInfo("wrong file name (head=A-Z)");
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
				showInfo("wrong file name (char=A-Z0-9)");
				return false;
			}
			for (int i = 0; i < fileList.size(); i++)
			{
				if (file.equals(fileList.getString(i)))
				{
					showInfo("wrong file name (duplicate)");
					return false;
				}
			}
			fileName = file;
			isNewFile = true;
		}
		else if (saveType.getSelectedIndex() == 1) // LOAD
		{
			if (fileList.size() == 0)
			{
				showInfo("no file");
				return false;
			}
			int idx = fileList.getSelectedIndex();
			if (idx < 0)
			{
				showInfo("no selected");
				return false;
			}
			fileName = fileList.getString(idx);
			isNewFile = false;
		}
		else if (saveType.getSelectedIndex() == 2) // DELETE
		{
			String file = deleteName.getString();
			if (file == null)
			{
				showInfo("need file name");
				return false;
			}
			fileName = null;
			for (int i = 0; i < fileList.size(); i++)
			{
				if (file.equals(fileList.getString(i)))
				{
					fileName = file;
					break;
				}
			}
			if (fileName == null)
			{
				showInfo("not found file name");
				return false;
			}
			isNewFile = false;
		}
		setTicker(null);
		return true;
	}
	
	public boolean selectedDelete()
	{
		return saveType.getSelectedIndex() == 2;
	}
	
	public boolean doDelete()
	{
		if (fileName == null)
		{
			showInfo("fileName is null");
			return false;
		}
		saveType.setSelectedIndex(0, true);
		String rsName = "casl2." + fileName;
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
		showInfo("deleted " + fileName);
		fileName = null;
		deleteName.setString(null);
		return true;
	}
	
	public int saveSrc(String name, byte[] src)
	{
		if (name == null || src == null)
		{
			return -1;
		}
		String rsName = "casl2." + name;
		RecordStore rs = null;
		try
		{
			rs = RecordStore.openRecordStore(rsName, true);
			rs.addRecord(src, 0, src.length);
			fileList.append(name, null);
			int size = rs.getSizeAvailable();
			return size;
		}
		catch (RecordStoreException ex)
		{
			CASL2MIDlet.lastError = ex.toString();
			return -1;
		}		
		finally
		{
			if (rs != null)
			{
				try { rs.closeRecordStore(); }
				catch (Exception ex) {}
				rs = null;
			}
		}
	}

	public int saveSrc(String src)
	{
		if (curRS == null)
		{
			return -1;
		}
		if (src == null)
		{
			return -1;
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
			return curRS.getSizeAvailable();
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
			return -1;
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