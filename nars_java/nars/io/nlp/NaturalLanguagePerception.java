package nars.io.nlp;

import java.util.ArrayList;
import java.util.List;
import nars.core.control.AbstractTask;
import nars.io.narsese.Narsese;

public class NaturalLanguagePerception {
    static public class LinePart {
        public LinePart(EnumType type, String content) {
            this.type = type;
            this.content = content;
        }
        
        public EnumType type;
        public String content;
        
        public enum EnumType {
            SIGN,
            WORD
        }
    }
    
    static public List<AbstractTask> parseLine(String input, Narsese narsese, String prefix) {

        List<String> statements = new ArrayList();

        parseLineInternal(input, statements, prefix);
        
        List<AbstractTask> results = new ArrayList();
        for (String i : statements) {
            try {
                AbstractTask t = narsese.parseNarsese(new StringBuilder(i));
                if (t != null) {
                    results.add(t);
                }
            }
            catch (Narsese.InvalidInputException e) { }            
        }
        
        return results;
    }
    
    static private void parseLineInternal(String input, List<String> statements, String prefix) {
        ArrayList<LinePart> lineParts = parseIntoLineParts(input);
        
        // TODO< split the lineparts on dot and comma and semicolon >
        String naresesOfPartOfLine = translateLinepartsToNareses(lineParts, prefix);
        statements.add(naresesOfPartOfLine);
    }
    
    static public ArrayList<LinePart> parseIntoLineParts(String input) {
        String[] splitedInput = Utility.splitInclusive(input, splitSigns);
        ArrayList<LinePart> lineParts = new ArrayList<LinePart>();
        
        for (int splitedI = 0; splitedI < splitedInput.length; splitedI++) {
            String inputPart = splitedInput[splitedI];
            
            if (inputPart.equals(" ")) {
                continue;
            }
            
            if (inputPart.length() == 1) {
                if (isSplitSign(inputPart.charAt(0))) {
                    lineParts.add(new LinePart(LinePart.EnumType.SIGN, inputPart));
                }
                else {
                    lineParts.add(new LinePart(LinePart.EnumType.WORD, inputPart));
                }
            }
            else {
                lineParts.add(new LinePart(LinePart.EnumType.WORD, inputPart));
            }
        }
        
        return lineParts;
    }
    
    // helper for outside
    public static String reconstructString(ArrayList<LinePart> lineParts) {
        String resultString = "";
        
        for(LinePart linePart : lineParts) {
            resultString += linePart.content + " ";
        }
        
        return resultString;
    }
    
    // helper for outside
    public static ArrayList<ArrayList<LinePart>> splitLinePartsIntoSentenceParts(ArrayList<LinePart> lineParts) {
        // TODO< move this into NaturalLanguagePerception >
        ArrayList<LinePart> currentLine;
        
        ArrayList<ArrayList<LinePart>> resultList = new ArrayList<>();
        
        currentLine = new ArrayList<>();
        
        int i;
        
        for( i = 0; i < lineParts.size(); i++ ) {
            NaturalLanguagePerception.LinePart iterationlinePart = lineParts.get(i);
            
            currentLine.add(iterationlinePart);
            
            if (isSentenceSpliter(iterationlinePart)) {
                resultList.add(currentLine);
                
                currentLine = new ArrayList<>();
            }
        }
        
        if(currentLine.size() != 0) {
            resultList.add(currentLine);
            currentLine = new ArrayList<>();
        }
        
        return resultList;
    }
    
    private static boolean isSentenceSpliter(NaturalLanguagePerception.LinePart linePart) {
        if (linePart.type != NaturalLanguagePerception.LinePart.EnumType.SIGN) {
            return false;
        }
        
        return linePart.content.equals(".") || linePart.content.equals(",") || linePart.content.equals(";") || linePart.content.equals("!") || linePart.content.equals("?");
    }
    static private String translateLinepartsToNareses(ArrayList<LinePart> lineparts, String prefix) {
        String translated = "";
        
        int i = 0;
        for (LinePart iterationLinePart : lineparts) {
            boolean lastOne = i == (lineparts.size()-1);
            
            translated += translateLinepartToNareses(iterationLinePart, prefix);
            
            if (!lastOne) {
                translated += ", ";
            }
            
            i++;
        }
        
        return "<(*," + translated + ") --> linepart>. :|:";
    }
    
    static private String translateLinepartToNareses(LinePart linepart, String prefix) {
        if (linepart.type == LinePart.EnumType.SIGN) {
            return "'" + linepart.content + "'";
        }
        else {
            return withorWithoutPrefix(linepart.content, prefix);
        }
            
    }
    
    static private String withorWithoutPrefix(String content, String prefix) {
        if (prefix.isEmpty()) {
            return content;
        }
        return prefix + "-" + content;
    }
    
    static private boolean isSplitSign(char sign) {
        return Utility.containsChar(splitSigns, sign);
    }
    
    static private final char[] splitSigns = new char[]{' ', '.', ',', '?', '!', ';', ':', '/', '\\', '%', '(', ')', '-', '+', '*', '\'', '\"', '=', '{', '}', '(', ')', '<', '>'};
}