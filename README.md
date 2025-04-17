We have successfully ran the code in VScode with MacOS env.

## library used & Installation
java 21
spring boot 3.4.4
maven
jsoup 1.19.1
jdbm 1.0

VScode extensions needed (if you run in VScode):
1. Extension Pack for Java
2. Spring Boot Extension Pack

Frontend installation:
Node.js v20.16.0 (https://nodejs.org/en/download) (can use `node -v` to verify installation)


## Run the program
- ensure you open the `comp4321-project` folder

### Spring Boot
1. open a new terminal (ensure the current path is `comp4321-project` folder)
2. `cd searchengine`, change current path to the spring boot folder
3. `./mvnw spring-boot:run`, maven is used 

* you can verify the set up by visiiting `http://localhost:8080` and see a "Hello World" message


### React.js (frontend)
1. open a new terminal (ensure the current path is `comp4321-search-engine` folder)
2. `cd frontend`, change the current path to the react js folder
3. `npm install`, please make sure you have installed Node.js
4. `npm start`

* you can open `localhost:3000` and see the web portal

## Spring Boot End Point
1. **/crawl**: starting crawler with starting url & max page
2. **/crawled-pages**: get the detail of the crawled page in .db
3. **/clean-db**: delete the .db file
4. **/search/query**: searching with the given query
5. **/search/keywords**: get all stemmed keyword
6. **/search/hot-topic**: get the 5 most frequent search from global cache
7. **/search/clean-cache**: clear the global cache