Used library
- htmlparser.jar
- jdbm-1.0.jar
- jsoup-1.19.1.jar
- java 21.0.6

The command below is working in a macOS environment. Please change to the equivalent command for other operating systems.

1. Before Running the Code:
    1.1. make sure you have cd to the COMP4321-project directory
    1.2. Ensure there is no existing crawlerDB.db file in the COMP4321-project directory (otherwise it may affect the index result)

2. Run the Crawler (you can change the number of indexed pages and the starting URL in src/main/Main.java)
    2.1. javac -d bin -cp "lib/*" $(find src -name "*.java")
    2.2. java -cp "bin:lib/*" main.Main

3. Run the test program (Assuming that you have already run the Crawler)
    3.1. java -cp "bin:lib/*" main.testProgram

* The results of the test program will be outputted in spider_result.txt. There is no need to clear the file on successive runs; only the most recent results will be shown.