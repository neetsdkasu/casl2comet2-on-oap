
public class CometII
{
	
	final Memory memory;
	
	volatile Device stdin, stdout , stderr;
	
	int GR0, GR1, GR2, GR3, GR4, GR5, GR6, GR7;
	int SP, PR, FR;
	
	String op = "NOP";
	
	public CometII(byte[] pg)
	{
		memory = new Memory(0xFFFF << 1);
		PR = FR = 0; SP = 0xFFFF;
		
		stdin = null;
		stdout = null;
		stderr = null;
		
		if (pg != null)
		{
			try
			{
				memory.write(pg);
				memory.setPos(PR << 1);
			}
			catch (java.io.IOException ex)
			{
				throw new CometIIError(ex);
			}
		}
	}
	
	public void resetRegisters()
	{
		PR = FR = 0; SP = 0xFFFF;
		GR0 = GR1 = GR2 = GR3 = GR4 = GR5 = GR6 = GR7 = 0;
	}
	
	public void setDevices(Device[] devs)
	{
		stdin = stdout = stderr = null;
		if (devs == null)
		{
			return;
		}
		for (int i = 0; i < devs.length; i++)
		{
			switch (i)
			{
			case 0: stdin  = devs[i]; break;
			case 1: stdout = devs[i]; break;
			case 2: stderr = devs[i]; break;
			}
		}
	}
	
	public String[] getStatus()
	{
		return new String[]{
			"Last OP: " + op,
			"PR: " + PR + ", FR: " + FR + ", SP: " + SP,
			"GR0: " + GR0 + ", GR1: " + GR1 + ", GR2: " + GR2 + ", GR3: " + GR3,
			"GR4: " + GR4 + ", GR5: " + GR5 + ", GR6: " + GR6 + ", GR7: " + GR7
		};
	}
	
	public String toString()
	{
		return "Last OP: " + op + "\n"
			   + "PR: " + PR + ", FR: " + FR + ", SP: " + SP + "\n"
			   + "GR0: " + GR0 + ", GR1: " + GR1 + ", GR2: " + GR2 + ", GR3: " + GR3
			   + ", GR4: " + GR4 + ", GR5: " + GR5 + ", GR6: " + GR6 + ", GR7: " + GR7;
		
	}
	
	private int flag0a(int v)
	{
		if (v == 0)
		{
			FR = 0x4;
		}
		else
		{
			if ((v & 0x8000) == 0)
			{
				FR = 0x0;
			}
			else
			{
				FR = 0x2;
			}
			
			if (v > 0x7FFF || v < -0x8000)
			{
				FR |= 0x1;
			}
		}
		return v & 0xFFFF;
	}
	
	private int flag0l(int v)
	{
		if (v == 0)
		{
			FR = 0x4;
		}
		else if (v > 0xFFFF || v < 0)
		{
			FR = 0x1;
		}
		else
		{
			FR = 0x0;
		}
		if ((v & 0x8000) != 0)
		{
			FR |= 0x2;
		}
		return v & 0xFFFF;
	}
	
	private void flag1(int v)
	{
		if (v == 0)
		{
			FR = 0x4;
		}
		else if ((v & 0x8000) == 0)
		{
			FR = 0x0;		
		}
		else
		{
			FR = 0x2;
		}
	}
	
	private void flag2a(int ca, int v)
	{
		if (v == 0)
		{
			FR = 0x4;
		}
		else if ((v & 0x8000) == 0)
		{
			FR = 0;
		}
		else
		{
			FR = 0x2;
		}
		
		if (ca != 0)
		{
			FR |= 0x1;
		}
	}

	private void flag2l(int ca, int v)
	{
		if (v == 0)
		{
			FR = 0x4;
		}
		else if ((v & 0x8000) == 0)
		{
			FR = 0;
		}
		else
		{
			FR = 0x2;
		}
		
		if (ca != 0)
		{
			FR |= 0x1;
		}
	}
	
	private int calcAddr(Memory in, int code)
	{
		int adr = 0xFFFF & in.readShort(); PR++;
		switch (code & 0xF)
		{
		case 0x0: break;
		case 0x1: adr += GR1; break;
		case 0x2: adr += GR2; break;
		case 0x3: adr += GR3; break;
		case 0x4: adr += GR4; break;
		case 0x5: adr += GR5; break;
		case 0x6: adr += GR6; break;
		case 0x7: adr += GR7; break;
		default: throw new CometIIError(op);
		}
		adr &= 0xFFFF;
		op += ", addr: " + adr;
		return adr;
	}

	
	private int getAddr(Memory in, int code)
	{
		int adr = calcAddr(in, code);
		memory.setPos((adr) << 1);
		adr = 0xFFFF & in.readShort();
		memory.setPos(PR << 1);
		op += ", val: " + adr;
		return adr;
	}
	
	private int getReg(int code)
	{
		switch (code & 0xF)
		{
		case 0x0: return GR0;
		case 0x1: return GR1;
		case 0x2: return GR2;
		case 0x3: return GR3;
		case 0x4: return GR4;
		case 0x5: return GR5;
		case 0x6: return GR6;
		case 0x7: return GR7;
		default: throw new CometIIError(op);
		}
	}
	
	public void step()
	{
		memory.setPos(PR << 1);
		step(memory, memory);
	}
	
	private void step(Memory mIn, Memory mOut)
	{
		if (PR > 0xFFFF)
		{
			throw new CometIIError("no program");
		}
		int code = 0xFFFF & mIn.readShort(); PR++;
		int adr;
		switch (code >> 8)
		{
		case 0x00: op = "NOP: " + Integer.toString(code, 16); 
			break;
		case 0x10: op = "LD: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: flag1(GR0 = getAddr(mIn, code)); break;
			case 0x1: flag1(GR1 = getAddr(mIn, code)); break;
			case 0x2: flag1(GR2 = getAddr(mIn, code)); break;
			case 0x3: flag1(GR3 = getAddr(mIn, code)); break;
			case 0x4: flag1(GR4 = getAddr(mIn, code)); break;
			case 0x5: flag1(GR5 = getAddr(mIn, code)); break;
			case 0x6: flag1(GR6 = getAddr(mIn, code)); break;
			case 0x7: flag1(GR7 = getAddr(mIn, code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x11: op = "ST: " + Integer.toString(code, 16);
			memory.setPos((calcAddr(mIn, code)) << 1);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: mOut.writeShort(GR0); break;
			case 0x1: mOut.writeShort(GR1); break;
			case 0x2: mOut.writeShort(GR2); break;
			case 0x3: mOut.writeShort(GR3); break;
			case 0x4: mOut.writeShort(GR4); break;
			case 0x5: mOut.writeShort(GR5); break;
			case 0x6: mOut.writeShort(GR6); break;
			case 0x7: mOut.writeShort(GR7); break;
			default: throw new CometIIError(op);
			}
			memory.setPos(PR);
			break;
		case 0x12: op = "LAD: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: GR0 = calcAddr(mIn, code); break;
			case 0x1: GR1 = calcAddr(mIn, code); break;
			case 0x2: GR2 = calcAddr(mIn, code); break;
			case 0x3: GR3 = calcAddr(mIn, code); break;
			case 0x4: GR4 = calcAddr(mIn, code); break;
			case 0x5: GR5 = calcAddr(mIn, code); break;
			case 0x6: GR6 = calcAddr(mIn, code); break;
			case 0x7: GR7 = calcAddr(mIn, code); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x14: op = "LD: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: flag1(GR0 = getReg(code)); break;
			case 0x1: flag1(GR1 = getReg(code)); break;
			case 0x2: flag1(GR2 = getReg(code)); break;
			case 0x3: flag1(GR3 = getReg(code)); break;
			case 0x4: flag1(GR4 = getReg(code)); break;
			case 0x5: flag1(GR5 = getReg(code)); break;
			case 0x6: flag1(GR6 = getReg(code)); break;
			case 0x7: flag1(GR7 = getReg(code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x20: op = "ADDA: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: GR0 = flag0a((int)(short)GR0 + (int)(short)getAddr(mIn, code)); break;
			case 0x1: GR1 = flag0a((int)(short)GR1 + (int)(short)getAddr(mIn, code)); break;
			case 0x2: GR2 = flag0a((int)(short)GR2 + (int)(short)getAddr(mIn, code)); break;
			case 0x3: GR3 = flag0a((int)(short)GR3 + (int)(short)getAddr(mIn, code)); break;
			case 0x4: GR4 = flag0a((int)(short)GR4 + (int)(short)getAddr(mIn, code)); break;
			case 0x5: GR5 = flag0a((int)(short)GR5 + (int)(short)getAddr(mIn, code)); break;
			case 0x6: GR6 = flag0a((int)(short)GR6 + (int)(short)getAddr(mIn, code)); break;
			case 0x7: GR7 = flag0a((int)(short)GR7 + (int)(short)getAddr(mIn, code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x21: op = "SUBA: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: GR0 = flag0a((int)(short)GR0 - (int)(short)getAddr(mIn, code)); break;
			case 0x1: GR1 = flag0a((int)(short)GR1 - (int)(short)getAddr(mIn, code)); break;
			case 0x2: GR2 = flag0a((int)(short)GR2 - (int)(short)getAddr(mIn, code)); break;
			case 0x3: GR3 = flag0a((int)(short)GR3 - (int)(short)getAddr(mIn, code)); break;
			case 0x4: GR4 = flag0a((int)(short)GR4 - (int)(short)getAddr(mIn, code)); break;
			case 0x5: GR5 = flag0a((int)(short)GR5 - (int)(short)getAddr(mIn, code)); break;
			case 0x6: GR6 = flag0a((int)(short)GR6 - (int)(short)getAddr(mIn, code)); break;
			case 0x7: GR7 = flag0a((int)(short)GR7 - (int)(short)getAddr(mIn, code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x22: op = "ADDL: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: GR0 = flag0l(GR0 + getAddr(mIn, code)); break;
			case 0x1: GR1 = flag0l(GR1 + getAddr(mIn, code)); break;
			case 0x2: GR2 = flag0l(GR2 + getAddr(mIn, code)); break;
			case 0x3: GR3 = flag0l(GR3 + getAddr(mIn, code)); break;
			case 0x4: GR4 = flag0l(GR4 + getAddr(mIn, code)); break;
			case 0x5: GR5 = flag0l(GR5 + getAddr(mIn, code)); break;
			case 0x6: GR6 = flag0l(GR6 + getAddr(mIn, code)); break;
			case 0x7: GR7 = flag0l(GR7 + getAddr(mIn, code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x23: op = "SUBL: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: GR0 = flag0l(GR0 - getAddr(mIn, code)); break;
			case 0x1: GR1 = flag0l(GR1 - getAddr(mIn, code)); break;
			case 0x2: GR2 = flag0l(GR2 - getAddr(mIn, code)); break;
			case 0x3: GR3 = flag0l(GR3 - getAddr(mIn, code)); break;
			case 0x4: GR4 = flag0l(GR4 - getAddr(mIn, code)); break;
			case 0x5: GR5 = flag0l(GR5 - getAddr(mIn, code)); break;
			case 0x6: GR6 = flag0l(GR6 - getAddr(mIn, code)); break;
			case 0x7: GR7 = flag0l(GR7 - getAddr(mIn, code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x24: op = "ADDA: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: GR0 = flag0a((int)(short)GR0 + (int)(short)getReg(code)); break;
			case 0x1: GR1 = flag0a((int)(short)GR1 + (int)(short)getReg(code)); break;
			case 0x2: GR2 = flag0a((int)(short)GR2 + (int)(short)getReg(code)); break;
			case 0x3: GR3 = flag0a((int)(short)GR3 + (int)(short)getReg(code)); break;
			case 0x4: GR4 = flag0a((int)(short)GR4 + (int)(short)getReg(code)); break;
			case 0x5: GR5 = flag0a((int)(short)GR5 + (int)(short)getReg(code)); break;
			case 0x6: GR6 = flag0a((int)(short)GR6 + (int)(short)getReg(code)); break;
			case 0x7: GR7 = flag0a((int)(short)GR7 + (int)(short)getReg(code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x25: op = "SUBA: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: GR0 = flag0a((int)(short)GR0 - (int)(short)getReg(code)); break;
			case 0x1: GR1 = flag0a((int)(short)GR1 - (int)(short)getReg(code)); break;
			case 0x2: GR2 = flag0a((int)(short)GR2 - (int)(short)getReg(code)); break;
			case 0x3: GR3 = flag0a((int)(short)GR3 - (int)(short)getReg(code)); break;
			case 0x4: GR4 = flag0a((int)(short)GR4 - (int)(short)getReg(code)); break;
			case 0x5: GR5 = flag0a((int)(short)GR5 - (int)(short)getReg(code)); break;
			case 0x6: GR6 = flag0a((int)(short)GR6 - (int)(short)getReg(code)); break;
			case 0x7: GR7 = flag0a((int)(short)GR7 - (int)(short)getReg(code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x26: op = "ADDL: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: GR0 = flag0l(GR0 + getReg(code)); break;
			case 0x1: GR1 = flag0l(GR1 + getReg(code)); break;
			case 0x2: GR2 = flag0l(GR2 + getReg(code)); break;
			case 0x3: GR3 = flag0l(GR3 + getReg(code)); break;
			case 0x4: GR4 = flag0l(GR4 + getReg(code)); break;
			case 0x5: GR5 = flag0l(GR5 + getReg(code)); break;
			case 0x6: GR6 = flag0l(GR6 + getReg(code)); break;
			case 0x7: GR7 = flag0l(GR7 + getReg(code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x27: op = "SUBL: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: GR0 = flag0l(GR0 - getReg(code)); break;
			case 0x1: GR1 = flag0l(GR1 - getReg(code)); break;
			case 0x2: GR2 = flag0l(GR2 - getReg(code)); break;
			case 0x3: GR3 = flag0l(GR3 - getReg(code)); break;
			case 0x4: GR4 = flag0l(GR4 - getReg(code)); break;
			case 0x5: GR5 = flag0l(GR5 - getReg(code)); break;
			case 0x6: GR6 = flag0l(GR6 - getReg(code)); break;
			case 0x7: GR7 = flag0l(GR7 - getReg(code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x30: op = "AND: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: flag1(GR0 = GR0 & getAddr(mIn, code)); break;
			case 0x1: flag1(GR1 = GR1 & getAddr(mIn, code)); break;
			case 0x2: flag1(GR2 = GR2 & getAddr(mIn, code)); break;
			case 0x3: flag1(GR3 = GR3 & getAddr(mIn, code)); break;
			case 0x4: flag1(GR4 = GR4 & getAddr(mIn, code)); break;
			case 0x5: flag1(GR5 = GR5 & getAddr(mIn, code)); break;
			case 0x6: flag1(GR6 = GR6 & getAddr(mIn, code)); break;
			case 0x7: flag1(GR7 = GR7 & getAddr(mIn, code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x31: op = "OR: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: flag1(GR0 = GR0 | getAddr(mIn, code)); break;
			case 0x1: flag1(GR1 = GR1 | getAddr(mIn, code)); break;
			case 0x2: flag1(GR2 = GR2 | getAddr(mIn, code)); break;
			case 0x3: flag1(GR3 = GR3 | getAddr(mIn, code)); break;
			case 0x4: flag1(GR4 = GR4 | getAddr(mIn, code)); break;
			case 0x5: flag1(GR5 = GR5 | getAddr(mIn, code)); break;
			case 0x6: flag1(GR6 = GR6 | getAddr(mIn, code)); break;
			case 0x7: flag1(GR7 = GR7 | getAddr(mIn, code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x32: op = "XOR: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: flag1(GR0 = GR0 ^ getAddr(mIn, code)); break;
			case 0x1: flag1(GR1 = GR1 ^ getAddr(mIn, code)); break;
			case 0x2: flag1(GR2 = GR2 ^ getAddr(mIn, code)); break;
			case 0x3: flag1(GR3 = GR3 ^ getAddr(mIn, code)); break;
			case 0x4: flag1(GR4 = GR4 ^ getAddr(mIn, code)); break;
			case 0x5: flag1(GR5 = GR5 ^ getAddr(mIn, code)); break;
			case 0x6: flag1(GR6 = GR6 ^ getAddr(mIn, code)); break;
			case 0x7: flag1(GR7 = GR7 ^ getAddr(mIn, code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x34: op = "AND: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: flag1(GR0 = GR0 & getReg(code)); break;
			case 0x1: flag1(GR1 = GR1 & getReg(code)); break;
			case 0x2: flag1(GR2 = GR2 & getReg(code)); break;
			case 0x3: flag1(GR3 = GR3 & getReg(code)); break;
			case 0x4: flag1(GR4 = GR4 & getReg(code)); break;
			case 0x5: flag1(GR5 = GR5 & getReg(code)); break;
			case 0x6: flag1(GR6 = GR6 & getReg(code)); break;
			case 0x7: flag1(GR7 = GR7 & getReg(code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x35: op = "OR: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: flag1(GR0 = GR0 | getReg(code)); break;
			case 0x1: flag1(GR1 = GR1 | getReg(code)); break;
			case 0x2: flag1(GR2 = GR2 | getReg(code)); break;
			case 0x3: flag1(GR3 = GR3 | getReg(code)); break;
			case 0x4: flag1(GR4 = GR4 | getReg(code)); break;
			case 0x5: flag1(GR5 = GR5 | getReg(code)); break;
			case 0x6: flag1(GR6 = GR6 | getReg(code)); break;
			case 0x7: flag1(GR7 = GR7 | getReg(code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x36: op = "XOR: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: flag1(GR0 = GR0 ^ getReg(code)); break;
			case 0x1: flag1(GR1 = GR1 ^ getReg(code)); break;
			case 0x2: flag1(GR2 = GR2 ^ getReg(code)); break;
			case 0x3: flag1(GR3 = GR3 ^ getReg(code)); break;
			case 0x4: flag1(GR4 = GR4 ^ getReg(code)); break;
			case 0x5: flag1(GR5 = GR5 ^ getReg(code)); break;
			case 0x6: flag1(GR6 = GR6 ^ getReg(code)); break;
			case 0x7: flag1(GR7 = GR7 ^ getReg(code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x40: op = "CPA: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: flag0a((int)(short)GR0 - (int)(short)getAddr(mIn, code)); break;
			case 0x1: flag0a((int)(short)GR1 - (int)(short)getAddr(mIn, code)); break;
			case 0x2: flag0a((int)(short)GR2 - (int)(short)getAddr(mIn, code)); break;
			case 0x3: flag0a((int)(short)GR3 - (int)(short)getAddr(mIn, code)); break;
			case 0x4: flag0a((int)(short)GR4 - (int)(short)getAddr(mIn, code)); break;
			case 0x5: flag0a((int)(short)GR5 - (int)(short)getAddr(mIn, code)); break;
			case 0x6: flag0a((int)(short)GR6 - (int)(short)getAddr(mIn, code)); break;
			case 0x7: flag0a((int)(short)GR7 - (int)(short)getAddr(mIn, code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x41: op = "CPL: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: flag0l(GR0 - getAddr(mIn, code)); break;
			case 0x1: flag0l(GR1 - getAddr(mIn, code)); break;
			case 0x2: flag0l(GR2 - getAddr(mIn, code)); break;
			case 0x3: flag0l(GR3 - getAddr(mIn, code)); break;
			case 0x4: flag0l(GR4 - getAddr(mIn, code)); break;
			case 0x5: flag0l(GR5 - getAddr(mIn, code)); break;
			case 0x6: flag0l(GR6 - getAddr(mIn, code)); break;
			case 0x7: flag0l(GR7 - getAddr(mIn, code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x44: op = "CPA: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: flag0a((int)(short)GR0 - (int)(short)getReg(code)); break;
			case 0x1: flag0a((int)(short)GR1 - (int)(short)getReg(code)); break;
			case 0x2: flag0a((int)(short)GR2 - (int)(short)getReg(code)); break;
			case 0x3: flag0a((int)(short)GR3 - (int)(short)getReg(code)); break;
			case 0x4: flag0a((int)(short)GR4 - (int)(short)getReg(code)); break;
			case 0x5: flag0a((int)(short)GR5 - (int)(short)getReg(code)); break;
			case 0x6: flag0a((int)(short)GR6 - (int)(short)getReg(code)); break;
			case 0x7: flag0a((int)(short)GR7 - (int)(short)getReg(code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x45: op = "CPL: " + Integer.toString(code, 16);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: flag0l(GR0 - getReg(code)); break;
			case 0x1: flag0l(GR1 - getReg(code)); break;
			case 0x2: flag0l(GR2 - getReg(code)); break;
			case 0x3: flag0l(GR3 - getReg(code)); break;
			case 0x4: flag0l(GR4 - getReg(code)); break;
			case 0x5: flag0l(GR5 - getReg(code)); break;
			case 0x6: flag0l(GR6 - getReg(code)); break;
			case 0x7: flag0l(GR7 - getReg(code)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x50: op = "SLA: " + Integer.toString(code, 16);
			adr = getAddr(mIn, code);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: adr = GR0 << adr; flag2a(adr & 0x8000, GR0 = (GR0 & 0x8000) | (adr & 0x7FFF)); break;
			case 0x1: adr = GR1 << adr; flag2a(adr & 0x8000, GR1 = (GR1 & 0x8000) | (adr & 0x7FFF)); break;
			case 0x2: adr = GR2 << adr; flag2a(adr & 0x8000, GR2 = (GR2 & 0x8000) | (adr & 0x7FFF)); break;
			case 0x3: adr = GR3 << adr; flag2a(adr & 0x8000, GR3 = (GR3 & 0x8000) | (adr & 0x7FFF)); break;
			case 0x4: adr = GR4 << adr; flag2a(adr & 0x8000, GR4 = (GR4 & 0x8000) | (adr & 0x7FFF)); break;
			case 0x5: adr = GR5 << adr; flag2a(adr & 0x8000, GR5 = (GR5 & 0x8000) | (adr & 0x7FFF)); break;
			case 0x6: adr = GR6 << adr; flag2a(adr & 0x8000, GR6 = (GR6 & 0x8000) | (adr & 0x7FFF)); break;
			case 0x7: adr = GR7 << adr; flag2a(adr & 0x8000, GR7 = (GR7 & 0x8000) | (adr & 0x7FFF)); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x51: op = "SRA: " + Integer.toString(code, 16);
			adr = getAddr(mIn, code);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: flag2a((GR0 >> (adr - 1)) & 0x1, GR0 = (((~(0xFFFF >> adr)) * (GR0 >> 15)) | (GR0 >> adr)) & 0xFFFF); break;
			case 0x1: flag2a((GR1 >> (adr - 1)) & 0x1, GR1 = (((~(0xFFFF >> adr)) * (GR1 >> 15)) | (GR1 >> adr)) & 0xFFFF); break;
			case 0x2: flag2a((GR2 >> (adr - 1)) & 0x1, GR2 = (((~(0xFFFF >> adr)) * (GR2 >> 15)) | (GR2 >> adr)) & 0xFFFF); break;
			case 0x3: flag2a((GR3 >> (adr - 1)) & 0x1, GR3 = (((~(0xFFFF >> adr)) * (GR3 >> 15)) | (GR3 >> adr)) & 0xFFFF); break;
			case 0x4: flag2a((GR4 >> (adr - 1)) & 0x1, GR4 = (((~(0xFFFF >> adr)) * (GR4 >> 15)) | (GR4 >> adr)) & 0xFFFF); break;
			case 0x5: flag2a((GR5 >> (adr - 1)) & 0x1, GR5 = (((~(0xFFFF >> adr)) * (GR5 >> 15)) | (GR5 >> adr)) & 0xFFFF); break;
			case 0x6: flag2a((GR6 >> (adr - 1)) & 0x1, GR6 = (((~(0xFFFF >> adr)) * (GR6 >> 15)) | (GR6 >> adr)) & 0xFFFF); break;
			case 0x7: flag2a((GR7 >> (adr - 1)) & 0x1, GR7 = (((~(0xFFFF >> adr)) * (GR7 >> 15)) | (GR7 >> adr)) & 0xFFFF); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x52: op = "SLL: " + Integer.toString(code, 16);
			adr = getAddr(mIn, code);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: flag2l((GR0 << (adr - 1)) & 0x8000, GR0 = (GR0 << adr) & 0xFFFF); break;
			case 0x1: flag2l((GR1 << (adr - 1)) & 0x8000, GR1 = (GR1 << adr) & 0xFFFF); break;
			case 0x2: flag2l((GR2 << (adr - 1)) & 0x8000, GR2 = (GR2 << adr) & 0xFFFF); break;
			case 0x3: flag2l((GR3 << (adr - 1)) & 0x8000, GR3 = (GR3 << adr) & 0xFFFF); break;
			case 0x4: flag2l((GR4 << (adr - 1)) & 0x8000, GR4 = (GR4 << adr) & 0xFFFF); break;
			case 0x5: flag2l((GR5 << (adr - 1)) & 0x8000, GR5 = (GR5 << adr) & 0xFFFF); break;
			case 0x6: flag2l((GR6 << (adr - 1)) & 0x8000, GR6 = (GR6 << adr) & 0xFFFF); break;
			case 0x7: flag2l((GR7 << (adr - 1)) & 0x8000, GR7 = (GR7 << adr) & 0xFFFF); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x53: op = "SRL: " + Integer.toString(code, 16);
			adr = getAddr(mIn, code);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: flag2l((GR0 >> (adr - 1)) & 0x1, GR0 = GR0 >> adr); break;
			case 0x1: flag2l((GR1 >> (adr - 1)) & 0x1, GR1 = GR1 >> adr); break;
			case 0x2: flag2l((GR2 >> (adr - 1)) & 0x1, GR2 = GR2 >> adr); break;
			case 0x3: flag2l((GR3 >> (adr - 1)) & 0x1, GR3 = GR3 >> adr); break;
			case 0x4: flag2l((GR4 >> (adr - 1)) & 0x1, GR4 = GR4 >> adr); break;
			case 0x5: flag2l((GR5 >> (adr - 1)) & 0x1, GR5 = GR5 >> adr); break;
			case 0x6: flag2l((GR6 >> (adr - 1)) & 0x1, GR6 = GR6 >> adr); break;
			case 0x7: flag2l((GR7 >> (adr - 1)) & 0x1, GR7 = GR7 >> adr); break;
			default: throw new CometIIError(op);
			}
			break;
		case 0x61: op = "JMI: " + Integer.toString(code, 16);
			adr = calcAddr(mIn, code);
			if ((FR & 0x2) != 0)
			{
				PR = adr;
				memory.setPos(PR << 1);
			}
			break;
		case 0x62: op = "JNZ: " + Integer.toString(code, 16);
			adr = calcAddr(mIn, code);
			if ((FR & 0x4) == 0)
			{
				PR = adr;
				memory.setPos(PR << 1);
			}
			break;
		case 0x63: op = "JZE: " + Integer.toString(code, 16);
			adr = calcAddr(mIn, code);
			if ((FR & 0x4) != 0)
			{
				PR = adr;
				memory.setPos(PR << 1);
			}
			break;
		case 0x64: op = "JUMP: " + Integer.toString(code, 16);
			PR = calcAddr(mIn, code);
			memory.setPos(PR << 1);
			break;
		case 0x65: op = "JPL: " + Integer.toString(code, 16);
			adr = calcAddr(mIn, code);
			if ((FR & 0x6) == 0)
			{
				PR = adr;
				memory.setPos(PR << 1);
			}
			break;
		case 0x66: op = "JOV: " + Integer.toString(code, 16);
			adr = calcAddr(mIn, code);
			if ((FR & 0x1) != 0)
			{
				PR = adr;
				memory.setPos(PR << 1);
			}
			break;
		case 0x70: op = "PUSH: " + Integer.toString(code, 16);
			adr = calcAddr(mIn, code);
			SP--;
			memory.setPos(SP << 1);
			mOut.writeShort(adr);
			memory.setPos(PR << 1);
			break;
		case 0x71: op = "POP: " + Integer.toString(code, 16);
			if (SP == 0xFFFF)
			{
				throw new CometIIError("Stack is already Head !");
			}
			memory.setPos(SP << 1);
			switch ((code & 0xF0) >> 4)
			{
			case 0x0: GR0 = 0xFFFF & mIn.readShort(); break;
			case 0x1: GR1 = 0xFFFF & mIn.readShort(); break;
			case 0x2: GR2 = 0xFFFF & mIn.readShort(); break;
			case 0x3: GR3 = 0xFFFF & mIn.readShort(); break;
			case 0x4: GR4 = 0xFFFF & mIn.readShort(); break;
			case 0x5: GR5 = 0xFFFF & mIn.readShort(); break;
			case 0x6: GR6 = 0xFFFF & mIn.readShort(); break;
			case 0x7: GR7 = 0xFFFF & mIn.readShort(); break;
			default: throw new CometIIError(op);
			}
			SP++;
			memory.setPos(PR << 1);
			break;
		case 0x80: op = "CALL: " + Integer.toString(code, 16);
			adr = calcAddr(mIn, code);
			SP--;
			memory.setPos(SP << 1);
			mOut.writeShort(PR);
			PR = adr;
			memory.setPos(PR << 1);
			break;
		case 0x81: op = "RET: " + Integer.toString(code, 16);
			if (SP == 0xFFFF)
			{
				throw new CometIIError("Stack is already head!");
			}
			memory.setPos(SP << 1);
			PR = 0xFFFF & mIn.readShort();
			SP++;
			memory.setPos(PR << 1);
			break;
		case 0xF0: op = "SVC: " + Integer.toString(code, 16);
			adr = calcAddr(mIn, code);
			switch (adr)
			{
			case 0: // Shutdown system
				throw new CometIIError("halt");
			case 1: // Read Line
				if (stdin == null)
				{
					throw new CometIIError("no input device");
				}
				else
				{
					// simulate OS program
					int len = 0;
					stdin.setState(Device.STATE_PREPARE);
					stdin.sendCommand(1);
					memory.setPos(GR1 << 1);
					for (;;)
					{
						if (stdin.isEOF())
						{
							len = -1;
							break;
						}
						String buf = stdin.getBuffer();
						if (buf != null)
						{
							len = Math.min(256, buf.length());
							for (int k = 0; k < len; k++)
							{
								int b = buf.charAt(k);
								mOut.writeShort(b & 0xFF);
							}
							if (stdout != null)
							{
								stdout.putBuffer("? " + buf);
							}
							break;
						}
						try
						{
							Thread.sleep(300);
						}
						catch (InterruptedException _)
						{
							// no code
						}
					}
					/*
					for (;;)
					{
						int st;
						while ((st = stdin.getState()) == Device.STATE_PREPARE)
						{
							try
							{
								Thread.sleep(1);
							}
							catch (InterruptedException _)
							{
								
							}
						}
						if (st == Device.STATE_READY)
						{
							if (len < 255) 
							{
								int b = stdin.getData();
								mOut.writeShort(b);
								len++;
							}
							stdin.setState(Device.STATE_PREPARE);
						}
						else // if (st == Device.STATE_STANDBY || st == Device.STATE_ERROR)
						{
							break;
						}
					}
					*/
					memory.setPos(GR2 << 1);
					mOut.writeShort(len);
				}
				break;
				
			case 2: // Write String
				if (stdout == null)
				{
					throw new CometIIError("no output device");
				}
				else
				{
					// simulate OS program
					memory.setPos(GR2 << 1);
					int len = 0xFFFF & mIn.readShort();
					int bsize = 0;
					memory.setPos(GR1 << 1);
					{
						StringBuffer sb = new StringBuffer(len);
						for (int k = 0; k < len; k++)
						{
							int dt = mIn.readShort() & 0xFF;
							sb.append((char)dt);
						}
						stdout.putBuffer(sb.toString());
						sb = null;
					}
					/* stdout.sendCommand(len);
					for (;;)
					{
						int st;
						while ((st = stdout.getState()) == Device.STATE_PREPARE)
						{
							try
							{
								Thread.sleep(1);
							}
							catch (InterruptedException _)
							{
								
							}
						}
						if (st == Device.STATE_READY)
						{
							if (bsize < len) 
							{
								int dt = 0xFFFF & mIn.readShort();
								stdout.putData(dt);
								bsize++;
								stdout.setState(Device.STATE_PREPARE);
							}
							else
							{
								break;
							}
						}
						else // if (st == Device.STATE_STANDBY || st == Device.STATE_ERROR)
						{
							// System.out.println("piyo" + st + " siz:" + bsize + " len: " + len);
							break;
						}
					}
					stdout.sendCommand(0);
					*/ 
				}
				break;
			
			default:
				throw new CometIIError("non support: SVC " + Integer.toString(adr));
			}
			memory.setPos(PR << 1);
			break;
		
		
		// for tableswitch
		
		           case 0x01: case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07:
		case 0x08: case 0x09: case 0x0A: case 0x0B: case 0x0C: case 0x0D: case 0x0E: case 0x0F:

		                                 case 0x13:            case 0x15: case 0x16: case 0x17:
		case 0x18: case 0x19: case 0x1A: case 0x1B: case 0x1C: case 0x1D: case 0x1E: case 0x1F:


		case 0x28: case 0x29: case 0x2A: case 0x2B: case 0x2C: case 0x2D: case 0x2E: case 0x2F:

		                                 case 0x33:                                  case 0x37:
		case 0x38: case 0x39: case 0x3A: case 0x3B: case 0x3C: case 0x3D: case 0x3E: case 0x3F:

		                      case 0x42: case 0x43:                       case 0x46: case 0x47:
		case 0x48: case 0x49: case 0x4A: case 0x4B: case 0x4C: case 0x4D: case 0x4E: case 0x4F:

		                                            case 0x54: case 0x55: case 0x56: case 0x57:
		case 0x58: case 0x59: case 0x5A: case 0x5B: case 0x5C: case 0x5D: case 0x5E: case 0x5F:

		case 0x60:                                                                   case 0x67:
		case 0x68: case 0x69: case 0x6A: case 0x6B: case 0x6C: case 0x6D: case 0x6E: case 0x6F:

		                      case 0x72: case 0x73: case 0x74: case 0x75: case 0x76: case 0x77:
		case 0x78: case 0x79: case 0x7A: case 0x7B: case 0x7C: case 0x7D: case 0x7E: case 0x7F:

		                      case 0x82: case 0x83: case 0x84: case 0x85: case 0x86: case 0x87:
		case 0x88: case 0x89: case 0x8A: case 0x8B: case 0x8C: case 0x8D: case 0x8E: case 0x8F:

		case 0x90: case 0x91: case 0x92: case 0x93: case 0x94: case 0x95: case 0x96: case 0x97:
		case 0x98: case 0x99: case 0x9A: case 0x9B: case 0x9C: case 0x9D: case 0x9E: case 0x9F:

		case 0xA0: case 0xA1: case 0xA2: case 0xA3: case 0xA4: case 0xA5: case 0xA6: case 0xA7:
		case 0xA8: case 0xA9: case 0xAA: case 0xAB: case 0xAC: case 0xAD: case 0xAE: case 0xAF:

		case 0xB0: case 0xB1: case 0xB2: case 0xB3: case 0xB4: case 0xB5: case 0xB6: case 0xB7:
		case 0xB8: case 0xB9: case 0xBA: case 0xBB: case 0xBC: case 0xBD: case 0xBE: case 0xBF:

		case 0xC0: case 0xC1: case 0xC2: case 0xC3: case 0xC4: case 0xC5: case 0xC6: case 0xC7:
		case 0xC8: case 0xC9: case 0xCA: case 0xCB: case 0xCC: case 0xCD: case 0xCE: case 0xCF:

		case 0xD0: case 0xD1: case 0xD2: case 0xD3: case 0xD4: case 0xD5: case 0xD6: case 0xD7:
		case 0xD8: case 0xD9: case 0xDA: case 0xDB: case 0xDC: case 0xDD: case 0xDE: case 0xDF:

		case 0xE0: case 0xE1: case 0xE2: case 0xE3: case 0xE4: case 0xE5: case 0xE6: case 0xE7:
		case 0xE8: case 0xE9: case 0xEA: case 0xEB: case 0xEC: case 0xED: case 0xEE: case 0xEF:

		           case 0xF1: case 0xF2: case 0xF3: case 0xF4: case 0xF5: case 0xF6: case 0xF7:
		case 0xF8: case 0xF9: case 0xFA: case 0xFB: case 0xFC: case 0xFD: case 0xFE: case 0xFF:
		
		default: 
			throw new CometIIError("???: #" + Integer.toString(code, 16));
		}
	}
	
	public void run()
	{		
		try
		{
			memory.setPos(PR);
			for (;;)
			{
				step(memory, memory);
			}
		}
		catch (CometIIError ex)
		{
			if (stderr != null)
			{
				String msg = ex.toString();
				int len = msg.length();
				stderr.sendCommand(len);
				int bpos = 0;
				for (;;)
				{
					int st;
					while ((st = stderr.getState()) == Device.STATE_PREPARE)
					{
						try
						{
							Thread.sleep(1);
						}
						catch (InterruptedException _)
						{
							// no code
						}
					}
					if (st == Device.STATE_READY)
					{
						if (bpos < len) 
						{
							stderr.putData(msg.charAt(bpos));
							bpos++;
						}
						else
						{
							break; // invalid state
						}
						stderr.setState(Device.STATE_PREPARE);
					}
					else // if (st == Device.STATE_STANDBY || st == Device.STATE_ERROR)
					{
						break;
					}
				}
				stderr.sendCommand(0);
			}
			// ex.printStackTrace();
		}
	}
	
}