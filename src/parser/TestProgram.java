package parser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import compiler.LexicalAnalyzer;
import compiler.Token;

public class TestProgram {

   private static void showTokens(String inputFile) {
      Token tokenName;

      LexicalAnalyzer lex = new LexicalAnalyzer(inputFile, StandardCharsets.UTF_8);

      do {
         tokenName = lex.getToken();

         System.out.println("<" + tokenName.toString() + ">");
      } while (!tokenName.getName().equals("end_program"));

      System.out.println("");
   }

   private static boolean fileExists(String fileName) {
      File inputFile = new File (fileName);

      return inputFile.exists();
   }

   private static String readAllInputFileContents(String fileName, Charset charset) {
      String s = null;

      if (fileExists(fileName)) {
         try {
            byte [] fileContents = Files.readAllBytes(Paths.get(fileName));

            s = new String(fileContents, charset);
         } catch (IOException e) { }
      }

      return s;
   }

   public static void main(String[] args) {
      boolean verbose = true;

      String program = "program 1.txt";

      if (verbose)
         showTokens(program);

      SyntaxAnalyzer parser = new SyntaxAnalyzer(new LexicalAnalyzer(program, StandardCharsets.UTF_8));

      if (verbose)
         System.out.println(readAllInputFileContents(program, StandardCharsets.UTF_8) + "\n");

      if (parser.compile()) {
         System.out.println("Program compiled succesfully!");
         System.out.println("\nSymbol table \n\n" + parser.symbolTable());
         System.out.println("\nIntermediate code \n\n" + parser.intermediateCode());
      } else {
         System.out.println(parser.output());
      }
   }
}
