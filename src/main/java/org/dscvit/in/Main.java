package org.dscvit.in;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    Map<String, Set<String>> structstofunctions = new HashMap<>();
    Map<String, Set<String>> structstovariables = new HashMap<>();

    public static void main(String[] args) throws IOException {
        Main m = new Main();
        String inFile = args[0];
        String outFile = args[1];
        System.out.println(inFile);
        System.out.println(outFile);
        new File(outFile).createNewFile();
        String out = m.prototypeConverter(Files.readString(Paths.get(inFile)));
        Files.writeString(Paths.get(outFile), out);
        System.out.println(out);
    }

    static String PATTERN_NO_TYPEDEF = "struct (\\w+)\\s*?\\{[\\w\\W]*?(([\\w]*)? ([\\w]*)\\(\\) \\{([\\w\\W]*?)\\})[\\w\\W]*?\\};";

    static String PATTERN_NO_TYPDEF_ONLY = "struct (\\w+)\\s*?\\{([\\w\\W]*)\\};";
    static String PATTERN_FUNCTIONS_ONLY = "([\\w]*)? ([\\w]*)\\(\\) \\{([\\w\\W]*?)\\}";

//    static String PATTERN_LOOKAHEAD = "(?=(" + PATTERN_NO_TYPEDEF + ")).";
    private String prototypeConverter(String input) {

        //find all in this with struct st


        Pattern p = Pattern.compile(PATTERN_NO_TYPDEF_ONLY);
        Pattern p_inner = Pattern.compile(PATTERN_FUNCTIONS_ONLY);
        Matcher m = p.matcher(input);
        String output1 = m.replaceAll(matchResult -> {
            String structname = matchResult.group(1);
            structstofunctions.put(structname, new HashSet<>());
            Matcher m2 = p_inner.matcher(matchResult.group(2));
            Matcher m3 = p_inner.matcher(matchResult.group(2));
            Stream<String> funcs = m2.results().map(t -> {
                structstofunctions.get(structname).add(t.group(2));
                String func = t.group(1) + " " + "__dLang_" + structname + "_" + t.group(2) + "(struct " + matchResult.group(1) + " *this) {";
                func += t.group(3);
                func += "}\n";
                return func;
            });
            String leftout = m3.replaceAll(mr -> "");
            String fes = input.substring(matchResult.start(), matchResult.start(2)) + leftout + input.substring(matchResult.end(2), matchResult.end());
            String finalfinal = funcs.reduce(fes, (res, ele) -> res + "\n" + ele);
            return finalfinal;
        });

        //find variables of the detected structs

        for(Map.Entry<String, Set<String>> en: structstofunctions.entrySet()) {
            String vardetect = "\\s*?" + en.getKey() + "\\s*(\\w*)\\s*[;]";
            structstovariables.putIfAbsent(en.getKey(), new HashSet<>());
            Pattern p2 = Pattern.compile(vardetect);
            Matcher m2 = p2.matcher(output1);
            m2.results().forEach(t -> {
                structstovariables.get(en.getKey()).add(t.group(1));
            });

            vardetect = "\\s*?" + en.getKey() + "\\s*(\\w*)\\s*[=]\\s*[\\w\\W]*?[;]";
            p2 = Pattern.compile(vardetect);
            m2 = p2.matcher(output1);
            m2.results().forEach(t -> {
                structstovariables.get(en.getKey()).add(t.group(1));
            });
        }

        //find functions called from these structs and do the appropriate replacement
        //of the form a.funcname
        for(Map.Entry<String, Set<String>> en : structstovariables.entrySet()) {
            Set<String> funcs = structstofunctions.get(en.getKey());
            for(String var: en.getValue()) {
                String fundetect = var + ".(\\w*)\\(([\\w\\W]*?)\\)";
                Pattern p2 = Pattern.compile(fundetect);
                Matcher m2 = p2.matcher(output1);
                output1 = m2.replaceAll(t -> {
                    if(!funcs.contains(t.group(1))) {
                        return t.group(0);
                    }
                    //substitute with
                    //"__dLang_" + structname + "_" + t.group(2)
                    String rep = "__dLang_" + en.getKey() + "_" + t.group(1) + "(&" + var;
                    if(!t.group(2).isEmpty()) {
                        rep += ", " + t.group(2);
                    }
                    return rep + ")";
                });

                fundetect = var + "->(\\w*)\\(([\\w\\W]*?)\\)";
                p2 = Pattern.compile(fundetect);
                m2 = p2.matcher(output1);
                output1 = m2.replaceAll(t -> {
                    if(!funcs.contains(t.group(1))) {
                        return t.group(0);
                    }
                    //substitute with
                    //"__dLang_" + structname + "_" + t.group(2)
                    String rep = "__dLang_" + en.getKey() + "_" + t.group(1) + "(" + var;
                    if(!t.group(2).isEmpty()) {
                        rep += ", " + t.group(2);
                    }
                    return rep + ")";
                });
            }
        }


        return output1;
    }


}
