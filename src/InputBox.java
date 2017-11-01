import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

public class InputBox extends TextBox
{
	public InputBox()
	{
		super("input", "", 255, TextField.ANY);
	}
}