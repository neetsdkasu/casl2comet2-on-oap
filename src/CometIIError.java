public class CometIIError extends RuntimeException
{
	public CometIIError(String msg)
	{
		super(msg);
	}
	
	public CometIIError(Exception ex)
	{
		super(ex.toString());
	}
}