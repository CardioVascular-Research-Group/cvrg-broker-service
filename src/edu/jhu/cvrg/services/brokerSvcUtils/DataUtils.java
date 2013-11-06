package edu.jhu.cvrg.services.brokerSvcUtils;


import java.io.*;
import java.util.*;

import org.apache.axiom.om.OMElement;

public abstract class DataUtils {

	private static boolean verbose = true; 
    /**
     * fills the left side of a number with zeros <br>
     * e.g. zerofill(14, 3) -> "014" <br>
     * e.g. zerofill(187, 6) -> "000014"
     * Note: doesn't work with negative numbers
     **/
    public static String zerofill(int x, int d) {
		String s = "";
		switch (d) {
		case 7:
		    if (x<1000000) s += "0";
		case 6:
		    if (x<100000) s += "0";
		case 5:
		    if (x<10000) s += "0";
		case 4:
		    if (x<1000) s += "0";
		case 3:
		    if (x<100) s += "0";
		case 2:
		    if (x<10) s += "0";
		}
		return s+x;
    }


    public static void printIndent(PrintWriter out, int indent) {
    	out.print(indent(indent));
    }

    public static String indent(int indent) {
		switch (indent) {
		case 8:
		    return("        ");
		case 7:
		    return("       ");
		case 6:
		    return("      ");
		case 5:
		    return("     ");
		case 4:
		    return("    ");
		case 3:
		    return("   ");
		case 2:
		    return("  ");
		case 1:
		    return(" ");
		default:
		    StringBuffer buf = new StringBuffer();
		    for (int i=0; i<indent; ++i) { buf.append(" "); }
		    return buf.toString();
		}
    }


    public static String commaList(Iterator i) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		printCommaList(pw, i);
		pw.close();
		return sw.toString();
    }


    /**
     * Given an iterator, prints it as a comma-delimited list
     * (actually a comma-and-space delimited list).  E.g. If the
     * iterator contains the strings { "my", "dog", "has fleas" } it
     * will print "my, dog, has fleas".
     *
     * @param out the stream to write to
     * @param i an iterator containing printable (toString) objects, e.g. strings
     **/
    public static void printCommaList(PrintWriter out, Iterator i) {
		boolean first = true;
		while (i.hasNext()) {
		    if (first) first = false;
		    else out.print(", ");
		    out.print(i.next());
		}
    }

    /**
     * @returns true if all characters in the string are whitespace characters, or the string is empty
     **/
    public static boolean isWhitespace(String s) {
		for (int i=0; i<s.length(); ++i) {
		    if (!Character.isWhitespace(s.charAt(i))) return false;
		}
		return true;
    }

    /**
     * Class encapsulating information from an exec call -- slightly
     * easier than the standard API
     **/
    public static class ExecInfo {
		public int exit;
		public String stdout;
		public String stderr;
    }
    /**
     * Turn "Now is the time for all good men" into "Now is the time for..."
     * @param max maximum length of result string
     **/
    public static String abbreviate(String s, int max) {
		if (max < 4)
		    throw new IllegalArgumentException("Minimum abbreviation is 3 chars");
		if (s.length() < max) return s;
		// todo: break into words
		return s.substring(0, max-3) + "...";
    }

    /**
     * pad or truncate
     **/
    public static String pad(String s, int length) {
		if (s.length() < length) return s + indent(length - s.length());
		else return s.substring(0,length);
    }

    /**
     * returns the part of the second string from where it's different
     * from the first <p>
     * strdiff("i am a machine", "i am a robot") -> "robot"
     *
     **/
    public static String strdiff(String s1, String s2) {
		int i;
		for (i=0; i<s1.length() && i<s2.length(); ++i) {
		    if (s1.charAt(i) != s2.charAt(i)) {
			break;
		    }
		}
		if (i<s2.length())
		    return s2.substring(i);
		return "";
    }

    /**
     * count the number of occurences of ch inside s
     **/
    public static int count(String s, char ch) {
		int c=0;
		for (int i=0; i<s.length(); ++i) {
		    if (s.charAt(i) == ch) c++;
		}
		return c;
    }

    /**
     * Replace all occurences of target inside source with replacement.
     * E.g. replace("fee fie fo fum", "f", "gr") -> "gree grie gro grum"
     **/
    public static String replace(String source, String target, String replacement)
    {
		// could use a regular expression, but this keeps it portable
		StringBuffer result = new StringBuffer(source.length());
		int i = 0, j = 0;
		int len = source.length();
		while (i < len) {
		    j = source.indexOf(target, i);
		    if (j == -1) {
			result.append( source.substring(i,len) );
			break;
		    }
		    else {
			result.append( source.substring(i,j) );
			result.append( replacement );
			i = j + target.length();
		    }
		}
		return result.toString();
    }

    /**
     * <p>
     *  Trim the right spacing off of a <code>String</code>.
     * </p>
     *
     * @param orig <code>String</code> to rtrim.
     * @return <code>String</code> - orig with no right spaces
     */
    public static String rtrim(String orig) {
		int len = orig.length();
		int st = 0;
		int off = 0;
		char[] val = orig.toCharArray();
	
		while ((st < len) && (val[off + len - 1] <= ' ')) {
		    len--;
		}
		return ((st > 0) || (len < orig.length())) ? orig.substring(st, len) : orig;
    }

    /**
     * <p>
     *  Trim the left spacing off of a <code>String</code>.
     * </p>
     *
     * @param orig <code>String</code> to rtrim.
     * @return <code>String</code> - orig with no left spaces
     */
    public static String ltrim(String orig) {
		int len = orig.length();
		int st = 0;
		int off = 0;
		char[] val = orig.toCharArray();
	
		while ((st < len) && (val[off + st] <= ' ')) {
		    st++;
		}
		return ((st > 0) || (len < orig.length())) ? orig.substring(st, len) : orig;
    }

    /**
     * calculate the maximum length of all strings in i.  If i
     * contains other than strings, uses toString() value.
     **/
    public static int getMaxLength(Iterator i) {
		int max = 0;
		while (i.hasNext()) {
		    String s = i.next().toString();
		    int c = s.length();
		    if (c>max) max=c;
		}
		return max;
    }

    // see http://hotwired.lycos.com/webmonkey/reference/special_characters/
    static Object[][] entities = {
	{"#39", new Integer(39)},	// ' - apostrophe
	{"quot", new Integer(34)},	// " - double-quote
	{"amp", new Integer(38)},	// & - ampersand
	{"lt", new Integer(60)},	// < - less-than
	{"gt", new Integer(62)},	// > - greater-than
	{"nbsp", new Integer(160)},	// non-breaking space
	{"copy", new Integer(169)},	// � - copyright
	{"reg", new Integer(174)},	// � - registered trademark
	{"Agrave", new Integer(192)},	// � - uppercase A, grave accent
	{"Aacute", new Integer(193)},	// � - uppercase A, acute accent
	{"Acirc", new Integer(194)},	// � - uppercase A, circumflex accent
	{"Atilde", new Integer(195)},	// � - uppercase A, tilde
	{"Auml", new Integer(196)},	// � - uppercase A, umlaut
	{"Aring", new Integer(197)},	// � - uppercase A, ring
	{"AElig", new Integer(198)},	// � - uppercase AE
	{"Ccedil", new Integer(199)},	// � - uppercase C, cedilla
	{"Egrave", new Integer(200)},	// � - uppercase E, grave accent
	{"Eacute", new Integer(201)},	// � - uppercase E, acute accent
	{"Ecirc", new Integer(202)},	// � - uppercase E, circumflex accent
	{"Euml", new Integer(203)},	// � - uppercase E, umlaut
	{"Igrave", new Integer(204)},	// � - uppercase I, grave accent
	{"Iacute", new Integer(205)},	// � - uppercase I, acute accent
	{"Icirc", new Integer(206)},	// � - uppercase I, circumflex accent
	{"Iuml", new Integer(207)},	// � - uppercase I, umlaut
	{"ETH", new Integer(208)},	// � - uppercase Eth, Icelandic
	{"Ntilde", new Integer(209)},	// � - uppercase N, tilde
	{"Ograve", new Integer(210)},	// � - uppercase O, grave accent
	{"Oacute", new Integer(211)},	// � - uppercase O, acute accent
	{"Ocirc", new Integer(212)},	// � - uppercase O, circumflex accent
	{"Otilde", new Integer(213)},	// � - uppercase O, tilde
	{"Ouml", new Integer(214)},	// � - uppercase O, umlaut
	{"Oslash", new Integer(216)},	// � - uppercase O, slash
	{"Ugrave", new Integer(217)},	// � - uppercase U, grave accent
	{"Uacute", new Integer(218)},	// � - uppercase U, acute accent
	{"Ucirc", new Integer(219)},	// � - uppercase U, circumflex accent
	{"Uuml", new Integer(220)},	// � - uppercase U, umlaut
	{"Yacute", new Integer(221)},	// � - uppercase Y, acute accent
	{"THORN", new Integer(222)},	// � - uppercase THORN, Icelandic
	{"szlig", new Integer(223)},	// � - lowercase sharps, German
	{"agrave", new Integer(224)},	// � - lowercase a, grave accent
	{"aacute", new Integer(225)},	// � - lowercase a, acute accent
	{"acirc", new Integer(226)},	// � - lowercase a, circumflex accent
	{"atilde", new Integer(227)},	// � - lowercase a, tilde
	{"auml", new Integer(228)},	// � - lowercase a, umlaut
	{"aring", new Integer(229)},	// � - lowercase a, ring
	{"aelig", new Integer(230)},	// � - lowercase ae
	{"ccedil", new Integer(231)},	// � - lowercase c, cedilla
	{"egrave", new Integer(232)},	// � - lowercase e, grave accent
	{"eacute", new Integer(233)},	// � - lowercase e, acute accent
	{"ecirc", new Integer(234)},	// � - lowercase e, circumflex accent
	{"euml", new Integer(235)},	// � - lowercase e, umlaut
	{"igrave", new Integer(236)},	// � - lowercase i, grave accent
	{"iacute", new Integer(237)},	// � - lowercase i, acute accent
	{"icirc", new Integer(238)},	// � - lowercase i, circumflex accent
	{"iuml", new Integer(239)},	// � - lowercase i, umlaut
	{"eth", new Integer(240)},	// � - lowercase eth, Icelandic
	{"ntilde", new Integer(241)},	// � - lowercase n, tilde
	{"ograve", new Integer(242)},	// � - lowercase o, grave accent
	{"oacute", new Integer(243)},	// � - lowercase o, acute accent
	{"ocirc", new Integer(244)},	// � - lowercase o, circumflex accent
	{"otilde", new Integer(245)},	// � - lowercase o, tilde
	{"ouml", new Integer(246)},	// � - lowercase o, umlaut
	{"oslash", new Integer(248)},	// � - lowercase o, slash
	{"ugrave", new Integer(249)},	// � - lowercase u, grave accent
	{"uacute", new Integer(250)},	// � - lowercase u, acute accent
	{"ucirc", new Integer(251)},	// � - lowercase u, circumflex accent
	{"uuml", new Integer(252)},	// � - lowercase u, umlaut
	{"yacute", new Integer(253)},	// � - lowercase y, acute accent
	{"thorn", new Integer(254)},	// � - lowercase thorn, Icelandic
	{"yuml", new Integer(255)},	// � - lowercase y, umlaut
	{"euro", new Integer(8364)},	// Euro symbol
    };
    static Map e2i = new HashMap();
    static Map i2e = new HashMap();
    static {
		for (int i=0; i<entities.length; ++i) {
		    e2i.put(entities[i][0], entities[i][1]);
		    i2e.put(entities[i][1], entities[i][0]);
		}
    }

    /**
     * Turns funky characters into HTML entity equivalents<p>
     * e.g. <tt>"bread" & "butter"</tt> => <tt>&amp;quot;bread&amp;quot; &amp;amp; &amp;quot;butter&amp;quot;</tt>.
     * Update: supports nearly all HTML entities, including funky accents. See the source code for more detail.
     * @see #htmlunescape(String)
     **/
    public static String htmlescape(String s1)
    {
		StringBuffer buf = new StringBuffer();
		int i;
		for (i=0; i<s1.length(); ++i) {
		    char ch = s1.charAt(i);
		    String entity = (String)i2e.get( new Integer((int)ch) );
		    if (entity == null) {
			if (((int)ch) > 128) {
			    buf.append("&#" + ((int)ch) + ";");
			}
			else {
			    buf.append(ch);
			}
		    }
		    else {
			buf.append("&" + entity + ";");
		    }
		}
		return buf.toString();
    }

    /**
     * Turns unicode characters beyond the ASCII range into html escaped equiv.
     * Same as htmlescape, but allows entity characters to remain so that
     * html directives can be stored in the string.
     **/
    public static String unicodeescape(String s1)
    {
		StringBuffer buf = new StringBuffer();
		int i;
		for (i=0; i<s1.length(); ++i) {
		    char ch = s1.charAt(i);
			if (((int)ch) > 128) {
			    buf.append("&#" + ((int)ch) + ";");
			}
			else {
			    buf.append(ch);
			}
		}
		return buf.toString();
    }



    /**
     * Given a string containing entity escapes, returns a string
     * containing the actual Unicode characters corresponding to the
     * escapes.
     *
     * Note: nasty bug fixed by Helge Tesgaard (and, in parallel, by
     * Alex, but Helge deserves major props for emailing me the fix).
     *
     * @see #htmlescape(String)
     **/
    public static String htmlunescape(String s1) {
		StringBuffer buf = new StringBuffer();
		int i;
		for (i=0; i<s1.length(); ++i) {
		    char ch = s1.charAt(i);
		    if (ch == '&') {
			int semi = s1.indexOf(';', i+1);
			if (semi == -1) {
			    buf.append(ch);
			    continue;
			}
			String entity = s1.substring(i+1, semi);
			Integer iso;
			if (entity.charAt(0) == '#') {
			    iso = new Integer(entity.substring(1));
			}
			else {
			    iso = (Integer)e2i.get(entity);
			}
			if (iso == null) {
			    buf.append("&" + entity + ";");
	        }
			else {
			    buf.append((char)(iso.intValue()));
			}
			i = semi;
		    }
		    else {
			buf.append(ch);
		    }
		}
		return buf.toString();
    }

    /**
     * Prepares a string for output inside a JavaScript string,
     * e.g. for use inside a document.write("") command.
     *
     * Example:
     * <pre>
     * input string: He didn't say, "stop!"
     * output string: He didn\'t say, \"stop!\"
     * </pre>
     *
     * Deals with quotes and control-chars (tab, backslash, cr, ff, etc.)
     * Bug: does not yet properly escape Unicode / high-bit characters.
     *
     * @see #jsEscape(String, Writer)
     **/
    public static String jsEscape(String source) {
		try {
		    StringWriter sw = new StringWriter();
		    jsEscape(source, sw);
		    sw.flush();
		    return sw.toString();
		}
		catch (IOException ioe) {
		    // should never happen writing to a StringWriter
		    ioe.printStackTrace();
		    return null;
		}
    }


    /**
     * Prepares a string for output inside a JavaScript string,
     * e.g. for use inside a document.write("") command.
     *
     * Example:
     * <pre>
     * input string: He didn't say, "stop!"
     * output string: He didn\'t say, \"stop!\"
     * </pre>
     *
     * Deals with quotes and control-chars (tab, backslash, cr, ff, etc.)
     * Bug: does not yet properly escape Unicode / high-bit characters.
     *
     * @see #jsEscape(String)
     **/
    public static void jsEscape(String source, Writer out) throws IOException {
		char[] chars = source.toCharArray();
		for (int i=0; i<chars.length; ++i) {
		    char ch = chars[i];
		    switch (ch) {
		    case '\b':	// backspace (ASCII 8)
			out.write("\\b");
			break;
		    case '\t':	// horizontal tab (ASCII 9)
			out.write("\\t");
			break;
		    case '\n':	// newline (ASCII 10)
			out.write("\\n");
			break;
		    case 11:	// vertical tab (ASCII 11)
			out.write("\\v");
			break;
		    case '\f':	// form feed (ASCII 12)
			out.write("\\f");
			break;
		    case '\r':	// carriage return (ASCII 13)
			out.write("\\r");
			break;
		    case '"':	// double-quote (ASCII 34)
			out.write("\\\"");
			break;
		    case '\'':	// single-quote (ASCII 39)
			out.write("\\'");
			break;
		    case '\\':	// literal backslash (ASCII 92)
			out.write("\\\\");
			break;
		    default:
			// todo: escape unicode / high-bit chars (JS works
			// with either \ u 000 or \ x 000 -- both take hex codes
			// AFAIK)
			out.write(ch);
			break;
		    }
		}
    }



    /**
     *  Filter out Windows and Mac curly quotes, replacing them with
     *  the non-curly versions. Note that this doesn't actually do any
     *  checking to verify the input codepage. Instead it just
     *  converts the more common code points used on the two platforms
     *  to their equivalent ASCII values. As such, this method
     *  <B>should not be used</b> on ISO-8859-1 input that includes
     *  high-bit-set characters, and some text which uses other
     *  codepoints may be rendered incorrectly.
     *
     * @author Ian McFarland
     **/
    public static String uncurlQuotes(String input) {
		if (input==null)
		    return "";
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < input.length(); i++)
		{
		    char ch = input.charAt(i);
		    int code = (int) ch;
		    if (code == 210 || code == 211 || code == 147 || code == 148)
		    {
		    	ch = (char) 34; // double quote
		    }
		    else if (code == 212 || code == 213 || code == 145 || code == 146)
		    {
		    	ch = (char) 39; // single quote
		    }
		    sb.append(ch);
		}
		return sb.toString();
    }

    /**
     * capitalize the first character of s
     **/
    public static String capitalize(String s) {
    	return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    /**
     * lowercase the first character of s
     **/
    public static String lowerize(String s) {
    	return s.substring(0,1).toLowerCase() + s.substring(1);
    }

    /**
     * turn String s into a plural noun (doing the right thing with
     * "story" -> "stories" and "mess" -> "messes")
     **/
    public static String pluralize(String s) {
		if (s.endsWith("y"))
		    return s.substring(0, s.length()-1) + "ies";
	
		else if (s.endsWith("s"))
		    return s + "es";
	
		else
		    return s + "s";
    }

    public static boolean ok(String s) {
    	return (!(s == null || s.equals("")));
    }

    public static String toUnderscore(String s) {
		StringBuffer buf = new StringBuffer();
		char[] ch = s.toCharArray();
		for (int i=0; i<ch.length; ++i) {
		    if (Character.isUpperCase(ch[i])) {
			buf.append('_');
			buf.append(Character.toLowerCase(ch[i]));
		    }
		    else {
			buf.append(ch[i]);
		    }
		}
		return buf.toString();
    }

    public static String stripWhitespace(String s) {
		StringBuffer buf = new StringBuffer();
		char[] ch = s.toCharArray();
		for (int i=0; i<ch.length; ++i) {
		    if (Character.isWhitespace(ch[i])) {
			continue;
		    }
		    else {
			buf.append(ch[i]);
		    }
		}
		return buf.toString();
    }

    public static String getStackTrace(Throwable t) {
		StringWriter s = new StringWriter();
		PrintWriter p = new PrintWriter(s);
		t.printStackTrace(p);
		p.close();
		return s.toString();
    }

    public static void sleep(long msec) {
		try {
		    Thread.sleep(msec);
		}catch (InterruptedException ie) {}
    }


    public static final String VALUES_SEPARATOR = "~";
    public static final String YES_VALUES = "~1~yes~y~si~oui~hai~true";
    public static final String NO_VALUES = "~0~no~n~ie~false";
	/**
	 * Gets a boolean value from a yes/no string.
	 * Not case sensitive.
	 * Trailing and preceding whitespace ignored.
	 * Defaults to false.
	 * @return boolean
	 * @author Bill Girten
	 */
	public static boolean getBoolean(Object yesno) {
		boolean boo = false;
		try {
			if (yesno != null) {
			    String yn = yesno.toString();
				yn = VALUES_SEPARATOR + yn.trim().toLowerCase();
				if (NO_VALUES.indexOf(yn) > -1) {
					boo = false;
				} else if (YES_VALUES.indexOf(yn) > -1) {
					boo = true;
				}
			}
		} catch (Throwable ignored) {
			/* ignored */
		} finally {
			return boo;
		}
	}

	/**
	 * Insures that a String value is an empty String and not null.
	 * @return String 
	 * @author Bill Girten
	 */
	public static String getString(String str) {
    	if (str == null) {
    	    str = new String();
    	}
    	return str;
	}

	public static String plainToHtml(String str) {
	    String htmlString = new String();
        htmlString = htmlescape(str);
        htmlString = replace(htmlString, " ", "&nbsp;");
        htmlString = replace(htmlString, "\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
        htmlString = replace(htmlString, "\n", "<br>");
        htmlString = replace(htmlString, "jhu", "<font color=\"green\" size=\"+1\">jhu</font>");
        htmlString = "<pre>" + htmlString + "</pre>";
        return htmlString;
    }
    
    public static String plainToHTML(String str) {
        return plainToHtml(str);
    }

	/**
	 * Gets the tag along with it's attributes from the document passed in as a 
	 * String. eg. returns <head> or <body bgcolor="white">
	 * @return String
	 * @author Bill Girten
	 */
    public static String getTag(String tag, String doc) {
        int tagIndex, endIndex;
        String tagString = new String();
        // search for CAPITALIZED and lowerized tag
        tagIndex = getTagIndex(tag, doc);
        if (tagIndex > 0) {
            endIndex = doc.indexOf(">", tagIndex);
            if (tagIndex > 0) {
                tagString = doc.substring(tagIndex-1, endIndex+1);
            }
        }
        return tagString;
    }

    public static int getTagIndex(String tag, String doc) {
        int tagIndex;
        String tagString = tag;
        if (tag.charAt(0) != '<') {
            tagString = "<" + tagString;
        }
        // search for CAPITALIZED tag
        tagIndex = doc.indexOf(capitalize(tagString));
        if (tagIndex < 0) {
            // search for LOWERIZED tag
            tagIndex = doc.indexOf(lowerize(tagString));
        }
        return tagIndex;
    }

    public static int getTagIndex(String tag, String doc, int startIndex) {
        int tagIndex;
        String tagString = tag;
        if (tag.charAt(0) != '<') {
            tagString = "<" + tagString;
        }
        // search for CAPITALIZED tag
        
        tagIndex = doc.indexOf(capitalize(tagString), startIndex);
        if (tagIndex < 0) {
            // search for LOWERIZED tag
            tagIndex = doc.indexOf(lowerize(tagString), startIndex);
        }
        return tagIndex;
    }

    /**
	 * Gets the contents of the tag from the document passed in as a String.
	 * @return String
	 * @author Bill Girten
	 */
    public static String getTagContents(String tag, String doc) {
        int startIndex, endIndex;
        String tagString = new String();
        // search for CAPITALIZED and lowerized tag
        startIndex = getTagIndex(tag, doc);
        if (startIndex > 0) {
            startIndex = doc.indexOf(">", startIndex);
            endIndex = getTagIndex("</"+tag+">", doc, startIndex);
            if (startIndex > 0 && endIndex > 0) {
                tagString = doc.substring(startIndex+1, endIndex);
            }
        }
        return tagString;
    }

    /**
	 * Gets the contents of the tag from the document passed in as a String.
	 * @return String
	 * @author Bill Girten
	 */
    public static String replaceTag(String tag, String replaceIn,String replaceWith) {
        int startIndex, endIndex;
        StringBuffer replaced = new StringBuffer(replaceIn);
        // search for CAPITALIZED and lowerized tag
        startIndex = getTagIndex(tag, replaceIn);
        if (startIndex > 0) {
            endIndex = getTagIndex("</"+tag+">", replaceIn, startIndex);
            if (startIndex > 0 && endIndex > 0) {
                replaced.replace(startIndex-1, endIndex+tag.length()+3, 
                        replaceWith);
            }
        }
        return replaced.toString();
    }

    /**
	 * Returns the contents of the file as a String.
	 * @return String
	 * @author Bill Girten
	 */
    public static String getFileContents(String filepath) throws FileNotFoundException, IOException {
        FileReader filereader = new FileReader(filepath);
		BufferedReader bufferedreader = new BufferedReader(filereader);
		String lineread = "";
		StringBuffer contents = new StringBuffer();
		while ((lineread = bufferedreader.readLine()) != null){
			contents.append(lineread + "\n");
		}
		filereader.close();
		return contents.toString();
    }

	/** Parses a service's incoming XML and builds a Map of all the parameters for easy access.
	 * @param param0 - OMElement representing XML with the incoming parameters.
	 */
	public static Map<String, Object> buildParamMap(OMElement param0){
		debugPrintln("buildParamMap()");

		String key="";
		//		String sValue = null;
		Object oValue = null;
		//		OMElement omValue=null;

		Map<String, Object> paramMap = new HashMap<String, Object>();
		try {
			@SuppressWarnings("unchecked")
			Iterator<OMElement> iterator = param0.getChildren();

			while(iterator.hasNext()) {
				OMElement param = iterator.next();
				key = param.getLocalName();
				oValue = param.getText();
				if(oValue.toString().length()>0){
					//					value = sValue;
					debugPrintln(" - Key/Value: " + key + " / '" + oValue + "'");
					paramMap.put(key,oValue);
				}else{
					Iterator<OMElement> iterTester = param.getChildren();
					if(iterTester.hasNext()){
						OMElement omValue = (OMElement)param;
						paramMap.put(key,param);
						debugPrintln(" - Key/OMElement Value: " + key + " / " + omValue.getText()); // param.getText());
					}else{
						debugPrintln(" - Key/Blank: " + key + " / '" + oValue + "'");
						paramMap.put(key,"");	
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			debugPrintln("buildParamMap() failed.");
			return null;
		}

		debugPrintln("buildParamMap() found " + paramMap.size() + " parameters.");
		return paramMap;
	}

	private static void debugPrintln(String text){
		if(verbose)	System.out.println("++ DataUtils + " + text);
	}


}