// Memory

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Memory extends OutputStream
{
	public final DataOutputStream out;
	public final DataInputStream in;
	public final byte[] mem;
	private int pos = 0;
	
	public Memory(int size)
	{
		mem = new byte[size];
		out = new DataOutputStream(this);
		in = new DataInputStream(new ByteArrayInputStream(mem));
	}
	
	public int readShort()
	{
		try
		{
			return in.readShort();
		}
		catch (IOException ex)
		{
			throw new CometIIError(ex);
		}
	}
	
	public void writeShort(int v)
	{
		try
		{
			out.writeShort(v);
		}
		catch (IOException ex)
		{
			throw new CometIIError(ex);
		}
	}
	
	public int getOutPos()
	{
		return pos;
	}
	
	public void setPos(int pos)
	{
		setOutPos(pos);
		setInPos(pos);
	}
	
	public void setOutPos(int pos)
	{
		this.pos = pos;
	}
	
	public void setInPos(int pos)
	{
		try
		{
			in.reset();
			in.skipBytes(pos);
		}
		catch (IOException ex)
		{
			throw new CometIIError(ex);
		}
	}
	
	public void write(int b) throws IOException
	{
		mem[pos] = (byte)b;
		pos++;
	}
	
	public void write(byte[] b, int off, int len) throws IOException
	{
		while (len-- > 0)
		{
			mem[pos++] = b[off++];
		}
	}
	
	public void write(byte[] b) throws IOException
	{
		write(b, 0, b.length);
	}
}