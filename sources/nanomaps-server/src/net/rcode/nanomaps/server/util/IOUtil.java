package net.rcode.nanomaps.server.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.jboss.netty.util.CharsetUtil;

public class IOUtil {

	public static CharSequence slurpReader(Reader in) throws IOException {
		StringBuilder sb=new StringBuilder();
		char[] buffer=new char[4096];
		for (;;) {
			int r=in.read(buffer);
			if (r<0) break;
			sb.append(buffer, 0, r);
		}
		
		return sb;
	}

	public static CharSequence slurpFile(File file) throws IOException {
		Reader in=new InputStreamReader(new FileInputStream(file), CharsetUtil.UTF_8);
		try {
			return slurpReader(in);
		} finally {
			in.close();
		}
	}
	
	public static CharSequence loadXmlStandalone(File file) throws Exception {
		// Dom4j handles the full spattering of crazy entity stuff that mapnik xml
		// uses.  We bring it throug a DOM4j Document which is wasteful but avoids
		// subtle entity related problems I was having
		// TODO: Spend more time digging into this
		StringWriter out=new StringWriter((int)file.length() * 4);
		SAXReader saxReader=new SAXReader();
		Document doc=saxReader.read(file);
		XMLWriter xmlWriter=new XMLWriter(out);
		xmlWriter.write(doc);
		xmlWriter.flush();
		out.flush();
		
		return out.toString();
		
		/* The following is the JAXP way, which mysteriously totally botches entities that contain
		 * embedded XML
		StringWriter out=new StringWriter((int)file.length() * 4);
		Transformer transformer=TransformerFactory.newInstance().newTransformer();
		XMLReader saxReader=XMLReaderFactory.createXMLReader();
		
		InputSource inputSource=new InputSource(file.toString());
		SAXSource source=new SAXSource(saxReader, inputSource);
		StreamResult result=new StreamResult(out);
		
		transformer.transform(source, result);
		
		out.flush();
		return out.toString();
		*/
	}
}
