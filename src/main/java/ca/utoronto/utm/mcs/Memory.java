package ca.utoronto.utm.mcs;

public class Memory 
{
    private static long value = 0;
    private static String string = "";

    public long getValue() {
        return value;
    }
    
    public String getString() {
    	return string;
    }

    public void setValue(long newVal) {
        value = newVal;
    }
    
    public void setString (String newString) {
    	string = newString;
    }

    public Memory() {}
}
