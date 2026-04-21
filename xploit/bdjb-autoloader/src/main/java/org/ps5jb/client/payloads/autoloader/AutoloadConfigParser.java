package org.ps5jb.client.payloads.autoloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

final class AutoloadConfigParser {
    private AutoloadConfigParser() {
    }

    static List<String> parseCommands(File configFile) throws IOException {
        byte[] raw = FileIO.readAllBytes(configFile);
        String content = new String(raw, "UTF-8");

        List<String> commands = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new StringReader(content));

        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.length() == 0 || trimmed.startsWith("#")) {
                continue;
            }
            commands.add(trimmed);
        }

        return commands;
    }
}
