# Project README

README file for the project source code and instruction for this github repo.<br>
The readme.txt submitted for the project is in another file.


## Instructions to Run the Project

1. **Run the Crawler in Main.java:**
   ```sh
   javac -d bin -cp "lib/*" $(find src -name "*.java")
   java -cp "bin:lib/*" main.Main
   ```
You can delete the .db and .lg file (for jdbm) before each run (to re-index those pages)


## Folder Structure

- **lib/** 
  - Contains `.jar` files for different libraries

- **src/** 
  - Contains all the source code for the project
  - **main/**: Contains all the main functions codes
  - **test/**: Contains all the test program

- **bin/**
  - Contains the compiled output file


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
    *   **Value**: List<Integer> (List of Child Page IDs)
    

### childParentMap
    
*   **Description**: Maps child page IDs to lists of parent page IDs.
*   **Structure**:
    *   **Key**: Integer (Child Page ID)
    *   **Value**: List<Integer> (List of Parent Page IDs)
    

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
    *   **Value**: List<Posting> (refer to `Posting.java` for more info)
        

### titleIndex

*   **Description**: Maps word IDs to lists of postings for the title of the page.
*   **Structure**:
    *   **Key**: Integer (Word ID)
    *   **Value**: List<Posting> (refer to `Posting.java` for more info)
