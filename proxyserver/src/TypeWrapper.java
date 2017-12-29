
public class TypeWrapper {
	String headerType;
	boolean isContainContentLength;
	int ContentLengthValue;
	
	public TypeWrapper(String headerType, boolean isContainContentLength, int ContentLengthValue) {
		this.headerType = headerType;
		this.isContainContentLength = isContainContentLength;
		this.ContentLengthValue = ContentLengthValue;
	}
}
