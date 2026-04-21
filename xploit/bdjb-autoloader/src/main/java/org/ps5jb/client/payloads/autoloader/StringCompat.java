package org.ps5jb.client.payloads.autoloader;

final class StringCompat {
    private StringCompat() {
    }

    static String replaceLiteral(String source, String target, String replacement) {
        if (source == null || target == null || target.length() == 0 || replacement == null) {
            return source;
        }

        int start = 0;
        int idx = source.indexOf(target, start);
        if (idx < 0) {
            return source;
        }

        StringBuffer out = new StringBuffer(source.length());
        while (idx >= 0) {
            out.append(source.substring(start, idx));
            out.append(replacement);
            start = idx + target.length();
            idx = source.indexOf(target, start);
        }
        out.append(source.substring(start));
        return out.toString();
    }
}
