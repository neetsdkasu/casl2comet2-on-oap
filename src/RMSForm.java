import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException ;

public class RMSForm extends Form
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
		
		saveName = new TextField("new file:", "", 20, TextField.ANY);
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
			if (file == null || file.length() == 0)
			{
				info.setText("wrong file name");
				return false;
			}
			for (int i = 0; i < file.length(); i++)
			{
				int ch = file.charAt(i);
				if ('A' <= ch && ch <= 'Z')
				{
					continue;
				}
				if ('a' <= ch && ch <= 'z')
				{
					continue;
				}
				if ('0' <= ch && ch <= '9')
				{
					continue;
				}
				if (ch == '.')
				{
					continue;
				}
				info.setText("wrong file name (invalid char)");
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
		if (src == null)
		{
			return false;
		}
		if (curRS == null)
		{
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
		catch (RecordStoreException _)
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
			return false;
		}
	}
	
	public String getSrc()
	{
		if (fileName == null)
		{
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
			catch (RecordStoreException  _)
			{
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
			return new String(buf);
		}
		catch (RecordStoreException _)
		{
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
	
}