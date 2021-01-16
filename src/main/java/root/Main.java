package root;

public class Main {
    public static void main(String[] args) throws Exception {
        //JustParser.readInput(System.in);
    }

    public static void createParser(String input) throws Exception {
        System.out.println("input = \n" + input);
        JustParser.readInput(input);
    }

    public static void restParser() {
        JustParser.resetParser();
    }
}
