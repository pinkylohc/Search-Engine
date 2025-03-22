Used library
- htmlparser.jar
- jdbm-1.0.jar
- jsoup-1.19.1.jar
- java 21.0.6


Run the Crawler (for Mac/Linux, can change to equivalent cmd in window)
1. javac -d bin -cp "lib/*" $(find src -name "*.java")
2. java -cp "bin:lib/*" main.Main

Run the test program
(Assuming that you have already run the Crawler)
1. java -cp "bin:lib/*" main.testProgram

The results of the test program will be outputted in spiderResult.txt. There is no need to clear the file on successive runs; only the most recent results will be shown.
