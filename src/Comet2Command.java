	
	public class Comet2Command
	{
		public static final String[] ARG0_COMMANDS = {
			"NOP", "RET"
		};
		public static final int[] ARG0_CODES = {
			0x0000, 0x8100
		};
		public static final String[] ARG1_COMMANDS = {
			"POP"
		};
		public static final int[] ARG1_CODES = {
			0x7100
		};
		public static final String[] ARG2_COMMANDS = {
			"PUSH", "CALL", "SVC",
			"JMI", "JNZ", "JZE", "JUMP", "JPL", "JOV"
		};
		public static final int[] ARG2_CODES = {
			0x7000, 0x8000, 0xF000,
			0x6100, 0x6200, 0x6300, 0x6400, 0x6500, 0x6600
		};
		public static final String[] ARG3_COMMANDS = {
			"LD", "ST", "LAD", 
			"ADDA", "SUBA", "ADDL", "SUBL",
			"AND", "OR", "XOR",
			"CPA", "CPL",
			"SLA", "SRA", "SLL", "SRL"
		};
		public static final int[] ARG3_CODES = {
			0x1000, 0x1100, 0x1200,
			0x2000, 0x2100, 0x2200, 0x2300,
			0x3000, 0x3100, 0x3200,
			0x4000, 0x4100,
			0x5000, 0x5100, 0x5200, 0x5300
		};
		public static final String[][] COMMANDS = {
			ARG0_COMMANDS, ARG1_COMMANDS, ARG2_COMMANDS, ARG3_COMMANDS
		};
		public static final int[][] CODES = {
			ARG0_CODES, ARG1_CODES, ARG2_CODES, ARG3_CODES
		};

		public static int validCommandName(String name, Comet2Command cc)
		{
			for (int i = 0; i < COMMANDS.length; i++)
			{
				String[] cmdList = COMMANDS[i];
				for (int j = 0; j < cmdList.length; j++)
				{
					if (cmdList[j].equals(name))
					{
						cc.cmd = CODES[i][j];
						return i;
					}
				}
			}
			return -1;
		}
		
		public static boolean can2ndRegister(Comet2Command cc)
		{
			if (cc.cmd == 0x1000 || (0x2000 <= cc.cmd && cc.cmd < 0x5000))
			{
				cc.cmd |= 0x400;
				return true;
			}
			return false;
		}
		
		public int cmd = 0, pos = 0;
		public int arg1 = 0, arg2 = 0, arg3 = 0;
		public String label = null;
		public Comet2Command()
		{
		}
		
		public void clear()
		{
			cmd = pos = arg1 = arg2 = arg3 = 0;
			label = null;
		}
		
		public int getCode()
		{
			return cmd | (arg1 << 4) | arg3;
		}
		
	}
	