import org.bzdev.imageio.ImageMimeInfo;

/*
 * Program to print the MimeType line for a desktop-entry file
 * that will include the mime types EPTS can process.
 */
public class DesktopMimeLine {

    public static void main(String argv[]) {
	StringBuilder sb = new StringBuilder();

	sb.append("MimeType=application/x.epts+xml;");
	for (String mt: ImageMimeInfo.getMimeTypes()) {
	    mt = mt.replaceAll(";","\\;");
	    sb.append(mt);
	    sb.append(";");
	}
	System.out.println(sb.toString());
    }
}
