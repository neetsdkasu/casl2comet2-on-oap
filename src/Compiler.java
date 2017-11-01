// Compiler

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public final class Compiler
{
	private static final int STATE_HEAD       = 0;
	private static final int STATE_CMD        = 1;
	private static final int STATE_ARG1       = 2;
	private static final int STATE_ARG2_1     = 3;
	private static final int STATE_ARG2_2     = 4;
	private static final int STATE_ARG3_1     = 5;
	private static final int STATE_ARG3_2     = 6;
	private static final int STATE_ARG3_3     = 7;
	private static final int STATE_OUT_1      = 8;
	private static final int STATE_OUT_2      = 9;
	private static final int STATE_IN_1       = 10;
	private static final int STATE_IN_2       = 11;
	private static final int STATE_DS_SIZE    = 12;
	private static final int STATE_DC_VALUES  = 13;
	private static final int STATE_COMMENT    = 14;
	
	public Compiler()
	{
		
	}
	
	private int toRegeister(String name)
	{
		if (name != null && name.length() == 3 && name.charAt(0) == 'G' && name.charAt(1) == 'R')
		{
			int ch = name.charAt(2);
			if ('0' <= ch && ch <= '7')
			{
				return ch - '0';
			}
		}
		return -1;
	}
	
	private int toIndexRegister(String name)
	{
		if (name != null && name.length() == 3 && name.charAt(0) == 'G' && name.charAt(1) == 'R')
		{
			int ch = name.charAt(2);
			if ('1' <= ch && ch <= '7')
			{
				return ch - '0';
			}
		}
		return -1;
	}
	
	private String errmsg(int lines)
	{
		return errmsg(lines, null);
	}
	
	private String errmsg(int lines, String tok)
	{
		if (tok == null || tok.length() == 0)
		{
			return "syntax error (" + String.valueOf(lines) + ")";
		}
		return "syntax error (" + String.valueOf(lines) + "):" + tok;
	}
	
	private boolean isAlphabet(int ch)
	{
		return ('A' <= ch && ch <= 'Z') || ('a' <= ch && ch <= 'z');
	}
	
	private boolean isDigit(int ch)
	{
		return '0' <= ch && ch <= '9';
	}
	
	private boolean isValidLabel(String name)
	{
		if (toRegeister(name) >= 0)
		{
			return false;
		}
		if (name.length() > 8)
		{
			return false;
		}
		int ch = name.charAt(0);
		if (isAlphabet(ch))
		{
			for (int i = 1; i < name.length(); i++)
			{
				ch = name.charAt(i);
				if (!isAlphabet(ch) && !isDigit(ch))
				{
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	public String compile(Memory mem, String src)
	{
		if (src == null || src.length() == 0)
		{
			return "no source";
		}
		mem.setPos(0);
		int lines = 1;
		int mempos = 0;
		int state = STATE_HEAD;
		Vector cmdList = new Vector();
		Hashtable literals = new Hashtable();
		Hashtable labels = new Hashtable();
		Comet2Command cur = new Comet2Command();
		boolean comma = false;
		int dc_count = 0;
		for (int i = 0; i < src.length(); i++)
		{
			int ch = src.charAt(i);
			switch (ch)
			{
			case ',':
				switch (state)
				{
				case STATE_HEAD:
				case STATE_CMD:
				case STATE_ARG1:
				case STATE_ARG2_1:
				case STATE_ARG3_1:
				case STATE_OUT_1:
				case STATE_IN_1:
				case STATE_DS_SIZE:
					return errmsg(lines, "comma!");
				case STATE_DC_VALUES:
					if (dc_count == 0)
					{
						return errmsg(lines);
					}
					break;
				}
				if (comma)
				{
					return errmsg(lines);
				}
				comma = true;
				break;
			case ' ':
				if (state == STATE_HEAD)
				{
					state = STATE_CMD;
				}
				break;
			case ';':
			case '\r':
			case '\n':
				switch (state)
				{
				case STATE_HEAD:
				case STATE_COMMENT:
					break;
				case STATE_ARG2_2:
				case STATE_ARG3_3:
					// no ireg
					mem.writeShort(cur.getCode());
					mem.writeShort(cur.arg2);
					if (cur.label != null)
					{
						cmdList.addElement(cur);
						cur = new Comet2Command();
					}
					else
					{
						cur.clear();
					}
					mempos += 2;
					break;
				case STATE_DC_VALUES:
					if (dc_count == 0)
					{
						return errmsg(lines);
					}
					break;
				default:
					return errmsg(lines);
				}
				comma = false;
				if (ch == '\n')
				{
					state = STATE_HEAD;
					lines++;
				}
				else
				{
					state = STATE_COMMENT;
				}
				break;
			default:
				{
					int j = i + 1;
					while (j < src.length() && ";, \r\n".indexOf(src.charAt(j)) < 0)
					{
						j++;
					}
					String tk = src.substring(i, j);
					i = j - 1;
					switch (state)
					{
					case STATE_HEAD:
						if (isValidLabel(tk))
						{
							labels.put(tk, new Integer(mempos));
							state = STATE_CMD;
						}
						else
						{
							return errmsg(lines, tk);
						}
						break;
					case STATE_CMD:
						{
							cur.clear();
							int tp = Comet2Command.validCommandName(tk, cur);
							cur.pos = mempos;
							switch (tp)
							{
							case 0:
								mem.writeShort(cur.getCode());
								mempos++;
								cur.clear();
								state = STATE_COMMENT;
								break;
							case 1:
								state = STATE_ARG1;
								break;
							case 2:
								state = STATE_ARG2_1;
								break;
							case 3:
								state = STATE_ARG3_1;
								break;
							default:
								if ("START".equals(tk))
								{
									state = STATE_COMMENT;
								}
								else if ("END".equals(tk))
								{
									state = STATE_COMMENT;
								}
								else if ("DS".equals(tk))
								{
									dc_count = 0;
									state = STATE_DS_SIZE;
								}
								else if ("DC".equals(tk))
								{
									state = STATE_DC_VALUES;
								}
								else if ("OUT".equals(tk) || "IN".equals(tk))
								{
									cur.clear();
									Comet2Command.validCommandName("PUSH", cur);
									cur.arg3 = 1;
									mem.writeShort(cur.getCode());
									mem.writeShort(0);
									cur.arg3 = 2;
									mem.writeShort(cur.getCode());
									mem.writeShort(0);
									cur.clear();
									mempos += 4;
									state = ch == 'I' ? STATE_IN_1 : STATE_OUT_1;
								}
								else if ("RPUSH".equals(tk))
								{
									cur.clear();
									Comet2Command.validCommandName("PUSH", cur);
									for (int e = 1; e <= 7; e++)
									{
										cur.arg3 = e;
										mem.writeShort(cur.getCode());
										mem.writeShort(0);
										mempos += 2;
									}
									cur.clear();
									state = STATE_COMMENT;
								}
								else if ("RPOP".equals(tk))
								{
									cur.clear();
									Comet2Command.validCommandName("POP", cur);
									for (int e = 7; e > 0; e--)
									{
										cur.arg1 = e;
										mem.writeShort(cur.getCode());
										mempos++;
									}
									cur.clear();
									state = STATE_COMMENT;
								}
								else
								{
									return errmsg(lines, tk);
								}
								break;
							}
						}
						break;
					case STATE_ARG1:
					case STATE_ARG3_1:
						{
							int r = toRegeister(tk);
							if (r < 0)
							{
								return errmsg(lines, tk); 
							}
							cur.arg1 = r;
							switch (state)
							{
							case STATE_ARG1:
								mem.writeShort(cur.getCode());
								cur.clear();
								mempos++;
								state = STATE_COMMENT;
								break;
							case STATE_ARG3_1:
								state = STATE_ARG3_2;
								break;
							}
						}
						break;
					case STATE_ARG3_2:
						// addr or lit or label or reg
						if (!comma)
						{
							return errmsg(lines, "no comma ! " + tk);
						}
						else
						{
							int r = toRegeister(tk);
							if (r >= 0)
							{
								if (Comet2Command.can2ndRegister(cur))
								{
									cur.arg3 = r;
									mem.writeShort(cur.getCode());
									cur.clear();
									mempos++;
									state = STATE_COMMENT;
								}
								else
								{
									return errmsg(lines, tk);
								}
								break;
							}
						}
					case STATE_ARG2_1:
						// addr or lit or label
						switch (ch)
						{
						case '#':
							if (tk.length() < 2)
							{
								return errmsg(lines, tk);
							}
							try
							{
								cur.arg2 = Integer.parseInt(tk.substring(1), 16) & 0xFFFF;
							}
							catch (NumberFormatException _)
							{
								return errmsg(lines, tk);
							}
							break;
						case '=':
							if (tk.length() < 2)
							{
								return errmsg(lines, tk);
							}
							cur.label = tk;
							if (!literals.containsKey(tk))
							{
								literals.put(tk, new Integer(lines));
							}
							break;
						case '\'':
							// TODO
							switch (tk.length())
							{
							case 3:
								if (tk.charAt(2) != '\'')
								{
									return errmsg(lines, tk);
								}
								int d = tk.charAt(1);
								switch (d)
								{
								case '\'':
									return errmsg(lines, tk);
								}
								cur.arg2 = d;
								break;
							case 4:
								if (!"''''".equals(tk))
								{
									return errmsg(lines, tk);
								}
								cur.arg2 = ch;
								break;
							default:
								return errmsg(lines, tk);
							}
							break;
						default:
							if (isDigit(ch) || ch == '-')
							{
								try
								{
									cur.arg2 = Integer.parseInt(tk) & 0xFFFF;
								}
								catch (NumberFormatException _)
								{
									return errmsg(lines, tk);
								}
							}
							else if (isValidLabel(tk))
							{
								cur.label = tk;
							}
							else
							{
								return errmsg(lines, tk);
							}
						}
						switch (state)
						{
						case STATE_ARG2_1: state = STATE_ARG2_2; break;
						case STATE_ARG3_2: state = STATE_ARG3_3; break;
						default: state = STATE_COMMENT; break;
						}
						break;
					case STATE_ARG2_2:
					case STATE_ARG3_3:
						// ireg
						if (!comma)
						{
							return errmsg(lines, "no comma ! " + tk);
						}
						else
						{
							int r = toIndexRegister(tk);
							if (r < 0)
							{
								return errmsg(lines, tk);
								
							}
							cur.arg3 = r;
							mem.writeShort(cur.getCode());
							mem.writeShort(cur.arg2);
							mempos += 2;
							if (cur.label != null)
							{
								cmdList.addElement(cur);
								cur = new Comet2Command();
							}
							else
							{
								cur.clear();
							}
							state = STATE_COMMENT;
						}
						break;
					case STATE_OUT_2:
					case STATE_IN_2:
						if (!comma)
						{
							return errmsg(lines, "no comma ! " + tk);
						}
					case STATE_OUT_1:
					case STATE_IN_1:
						// label
						if (ch == '=' || isValidLabel(tk))
						{
							cur.clear();
							Comet2Command.validCommandName("LAD", cur);
							switch (state)
							{
							case STATE_OUT_1:
							case STATE_IN_1:
								cur.arg3 = 1;
								break;
							default:
								cur.arg3 = 2;
								break;
							}
							cur.pos = mempos;
							cur.label = tk;
							mem.writeShort(cur.getCode());
							mem.writeShort(0);
							if (ch == '=')
							{
								if (!literals.containsKey(tk))
								{
									literals.put(tk, new Integer(lines));
								}
							}
							cmdList.addElement(cur);
							cur = new Comet2Command();
							mempos += 2;
							switch (state)
							{
							case STATE_OUT_1:
								state = STATE_OUT_2;
								break;
							case STATE_IN_1:
								state = STATE_IN_2;
								break;
							case STATE_OUT_2:
							case STATE_IN_2:
								Comet2Command.validCommandName("SVC", cur);
								mem.writeShort(cur.getCode());
								mem.writeShort(state == STATE_OUT_2 ? 2 : 1);
								Comet2Command.validCommandName("POP", cur);
								cur.arg1 = 2;
								mem.writeShort(cur.getCode());
								cur.arg1 = 1;
								mem.writeShort(cur.getCode());
								cur.clear();
								mempos += 4;
								state = STATE_COMMENT;
								break;
							}
						}
						else
						{
							return errmsg(lines, tk);
						}
						break;
					case STATE_DS_SIZE:
						// addr
						if (ch == '-')
						{
							return errmsg(lines, tk);
						}
						try
						{
							int sz;
							if (ch == '#')
							{
								sz = Integer.parseInt(tk.substring(1), 16) & 0xFFFF;
							}
							else
							{
								sz = Integer.parseInt(tk) & 0xFFFF;
							}
							for (int e = 0; e < sz; e++)
							{
								mem.writeShort(0);
							}
							mempos += sz;
							state = STATE_COMMENT;
						}
						catch (NumberFormatException _)
						{
							return errmsg(lines, tk);
						}
						break;
					case STATE_DC_VALUES:
						// value
						if (dc_count > 0 && !comma)
						{
							return errmsg(lines, "no comma ! " + tk);
						}
						dc_count++;
						switch (ch)
						{
						case '#':
							if (tk.length() < 2)
							{
								return errmsg(lines, tk);
							}
							try
							{
								int v = Integer.parseInt(tk.substring(1), 16) & 0xFFFF;
								mem.writeShort(v);
								mempos++;
							}
							catch (NumberFormatException _)
							{
								return errmsg(lines, tk);
							}
							break;
						case '\'':
							if (tk.length() < 3)
							{
								return errmsg(lines, tk);
							}
							if (tk.charAt(tk.length() - 1) != '\'')
							{
								return errmsg(lines, tk);
							}
							for (int e = 1; e < tk.length() - 1; e++)
							{
								int d = tk.charAt(e);
								if (d == '\'')
								{
									if (e + 1 >= tk.length() - 1 || tk.charAt(e + 1) != '\'')
									{
										return errmsg(lines, tk);
									}
									e++;
								}
								mem.writeShort(d);
								mempos++;
							}
							break;
						default:
							if (ch == '-' || isDigit(ch))
							{
								try
								{
									int v = Integer.parseInt(tk) & 0xFFFF;
									mem.writeShort(v);
									mempos++;
								}
								catch (NumberFormatException _)
								{
									return errmsg(lines, tk);
								}
							}
							else if (isValidLabel(tk))
							{
								cur.clear();
								cur.pos = mempos;
								cur.cmd = -1;
								cur.label = tk;
								cmdList.addElement(cur);
								cur = new Comet2Command();
								mem.writeShort(0);
								mempos++;
							}
							else
							{
								return errmsg(lines, tk);
							}
						}
						break;
					case STATE_COMMENT:
						break;
					}
				}
				comma = false;
				break;
			}
		}
		{	// last check
			switch (state)
			{
			case STATE_HEAD:
			case STATE_COMMENT:
				break;
			case STATE_ARG2_2:
			case STATE_ARG3_3:
				// no ireg
				mem.writeShort(cur.getCode());
				mem.writeShort(cur.arg2);
				if (cur.label != null)
				{
					cmdList.addElement(cur);
					cur = new Comet2Command();
				}
				else
				{
					cur.clear();
				}
				mempos += 2;
				break;
			case STATE_DC_VALUES:
				if (dc_count == 0)
				{
					return errmsg(lines);
				}
				break;
			default:
				return errmsg(lines);
			}
		}
		for (Enumeration en = literals.keys(); en.hasMoreElements(); )
		{
			String lit = (String)en.nextElement();
			if (lit.length() < 2)
			{
				Integer i = (Integer)literals.get(lit);
				return errmsg(i.intValue(), lit);
			}
			int ch = lit.charAt(1);
			switch (ch)
			{
			case '#':
				if (lit.length() < 3)
				{
					Integer i = (Integer)literals.get(lit);
					return errmsg(i.intValue(), lit);
				}
				try
				{
					int v = Integer.parseInt(lit.substring(2), 16) & 0xFFFF;
					labels.put(lit, new Integer(mempos));
					mem.writeShort(v);
					mempos++;
				}
				catch (NumberFormatException _)
				{
					Integer i = (Integer)literals.get(lit);
					return errmsg(i.intValue(), lit);
				}
				break;
			case '\'':
				if (lit.length() < 3)
				{
					Integer i = (Integer)literals.get(lit);
					return errmsg(i.intValue(), lit);
				}
				if (lit.charAt(lit.length() - 1) != '\'')
				{
					Integer i = (Integer)literals.get(lit);
					return errmsg(i.intValue(), lit);
				}
				labels.put(lit, new Integer(mempos));
				for (int e = 2; e < lit.length() - 1; e++)
				{
					int d = lit.charAt(e);
					if (d == '\'')
					{
						if (e + 1 >= lit.length() - 1 || lit.charAt(e + 1) != '\'')
						{
							Integer i = (Integer)literals.get(lit);
							return errmsg(i.intValue(), lit);
						}
						e++;
					}
					mem.writeShort(d);
					mempos++;
				}
				break;
			default:
				if (isDigit(ch))
				{
					try
					{
						int v = Integer.parseInt(lit.substring(1)) & 0xFFFF;
						labels.put(lit, new Integer(mempos));
						mem.writeShort(v);
						mempos++;
					}
					catch (NumberFormatException _)
					{
						Integer i = (Integer)literals.get(lit);
						return errmsg(i.intValue(), lit);
					}
				}
				else
				{
					Integer i = (Integer)literals.get(lit);
					return errmsg(i.intValue(), lit);
				}
				break;
			}
		}
		for (Enumeration en = cmdList.elements(); en.hasMoreElements(); )
		{
			Comet2Command cc = (Comet2Command)en.nextElement();
			Integer adr = (Integer)labels.get(cc.label);
			if (cc.cmd < 0)
			{
				mem.setPos(cc.pos << 1);
				mem.writeShort(adr.intValue() << 1);
			}
			else
			{
				int p = cc.pos + 1;
				mem.setPos(p << 1);
				mem.writeShort(adr.intValue() << 1);
			}
		}
		/*
		System.out.println("cmdList: " + cmdList.size());
		System.out.println("labels: " + labels.size());
		System.out.println("literals: " + literals.size());
		System.out.println("mempos: " + mempos);
		System.out.println("pos: " +  mem.getOutPos());
		*/
		return null;
	}
}