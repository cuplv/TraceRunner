package edu.colorado.plv;

import java.util.regex.Pattern;

/**
 * Created by s on 10/26/15.
 */
public class GlobUtil {
    public static Pattern createRegexFromGlob(String glob)
    {
        String out = "^";
        for(int i = 0; i < glob.length(); ++i)
        {
            final char c = glob.charAt(i);
            switch(c)
            {
                case '*': out += ".*"; break;
                case '?': out += '.'; break;
                case '.': out += "\\."; break;
                case '\\': out += "\\\\"; break;
                default: out += c;
            }
        }
        out += '$';
        return Pattern.compile(out);
    }
}
