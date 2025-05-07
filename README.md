### The code has ran successfully in MacOS with VScode

## library used & Installation
java 21 <br>
spring boot 3.4.4 <br>
maven <br>
jsoup 1.19.1 <br>
jdbm 1.0 <br>
react

VScode extensions needed (if you run in VScode):
1. Extension Pack for Java
2. Spring Boot Extension Pack

Frontend installation:
1. Node.js v20.16.0 (https://nodejs.org/en/download) (can use `node -v` to verify installation)


## Run the program
### 1. ensure you open the `comp4321-project` folder as root

### 2. Run the Spring Boot
1. open a new terminal (ensure the current path is `comp4321-project` folder)
2. `cd searchengine`, change current path to the spring boot folder
3. `./mvnw spring-boot:run`, maven is used 

* you can verify the set up by visiting `http://localhost:8080` and see a "Hello World" message


### 3. Run the React.js (frontend)
1. open a new terminal (ensure the current path is `comp4321-project` folder)
2. `cd frontend`, change the current path to the react js folder
3. `npm install`, please make sure you have installed Node.js
4. `npm start`

* you can open `localhost:3000` and see the web portal
* remark: we have use local storage to store some history records (you may need to use the morden broswers to ensure local storage is available) && ensure you don't have a previous local storage with item name ('searchHistory' and 'searchProfile')

### 4. Run the Crawler
1. please run the crawler before perfrom any searching
2. In our web portal header, click 'Crawler' in header bar
3. Navigate to the crawler page and use the crawler form below to start (this may take a few minutes)

### 5. Explore the Web Portal
<br>







## Spring Boot End Point
1. **/crawl**: starting crawler with starting url & max page
2. **/crawled-pages**: get the detail of the crawled page in .db
3. **/clean-db**: delete the .db file
4. **/search/query**: searching with the given query
5. **/search/keywords**: get all stemmed keyword
6. **/search/hot-topic**: get the 5 most frequent search from global cache
7. **/search/clean-cache**: clear the global cache

## Database Schema (HTree)
**Record Manager Name: crawlerDB**

### pageMap

*   **Description**: Maps URLs to page IDs.
*   **Structure**:
    *   **Key**: String (URL)
    *   **Value**: Integer (Page ID)
        

### pageidMap

*   **Description**: Maps page IDs to URLs.
*   **Structure**:
    *   **Key**: Integer (Page ID)
    *   **Value**: String (URL)
        

### pageIndex

*   **Description**: Stores PageInfo objects, which contain information about each **indexed** page.
*   **Structure**:
    *   **Key**: Integer (Page ID)
    *   **Value**: PageInfo (refer to `PageInfo.java` for more info)<br>
_** don’t use the pageIndex to retrieve ‘URL’ for child page, use mapping table instead, as it doesn’t include some of the child page info **_

### parentChildMap

*   **Description**: Maps parent page IDs to lists of child page IDs.
*   **Structure**:
    *   **Key**: Integer (Parent Page ID)
    *   **Value**: List `<Integer>` (List of Child Page IDs)
    

### childParentMap
    
*   **Description**: Maps child page IDs to lists of parent page IDs.
*   **Structure**:
    *   **Key**: Integer (Child Page ID)
    *   **Value**: List `<Integer>` (List of Parent Page IDs)
    

### wordMap
    
*   **Description**: Maps words to word IDs.
*   **Structure**:
    *   **Key**: String (Word)
    *   **Value**: Integer (Word ID)

### wordidMap
    
*   **Description**: Maps word IDs to words.
*   **Structure**:
    *   **Key**: Integer (Word ID)
    *   **Value**: String (Word)
        

### bodyIndex

*   **Description**: Maps word IDs to lists of postings for the body of the page.
*   **Structure**:
    *   **Key**: Integer (Word ID)
    *   **Value**: List `<Posting>` (refer to `Posting.java` for more info)
        

### titleIndex

*   **Description**: Maps word IDs to lists of postings for the title of the page.
*   **Structure**:
    *   **Key**: Integer (Word ID)
    *   **Value**: List `<Posting>` (refer to `Posting.java` for more info)


### pageRank

*   **Description**: Store the PageRank score for each indexed page
*   **Structure**:
    *   **Key**: Integer (Page ID)
    *   **Value**: Integer (PR Score)

### searchCache

*   **Description**: Maps word IDs to lists of postings for the title of the page.
*   **Structure**:
    *   **Key**: String (Query)
    *   **Value**: List `<PageResult>` 


### cacheMetadata

*   **Description**: Maps word IDs to lists of postings for the title of the page.
*   **Structure**:
    *   **Key**: String (Query)
    *   **Value**: Date (last Accessed), Integer (Frequency)

