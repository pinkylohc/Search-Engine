
//page rank from quality and authority
//location information and visual presentation?

//page rank = (1-d) + d(sum of page rank/number of outgoing links, for each ingoing page)
//d: damping factor -> set as variable
// all start with initial value 1

//synchronous: update all at once
//asynchronous: update sequentially
//set as toggle?

//also base on anchor text??
//also base on hit list (properties of the hit??)

//table PRScore
//key: docID, value: score
//initialize at 1

//variable damp (0-1)

//iteration stops after either x times
//or when the order does not change
// or with a stopping criterion
//sum of the absolute change in values < 0.0001 or sth

import java.io.IOException;
import java.io.FileWriter;
import java.util.*;

import jdbm.helper.FastIterator;
import jdbm.htree.HTree;
import main.database.DbManage;
import main.database.PageInfo;

public class pageRank{

    public static void main(String[] args) throws IOException {

        try{
            //initialization
            DbManage dbManage = new DbManage("crawlerDB");
            HTree score = loadOrCreateHTree("score");
            Float damp = 0.2f; //between 0 and 1
            Integer maxIterations = 10;
            Float maxError = 0.0001f;

            FastIterator pageIndexes = dbManage.printPageIndex.keys();
            FastIterator iter = pageIndexes;
            Integer pageId;
            while ((pageId = (Integer) iter.next()) != null) {
                score.put(pageId,1);
            }

            //asynchronous
            for (int i = 0; i < maxIterations; i++) {

                iter = pageIndexes;
                //Double errorSum = 0.0;

                while ((pageId = (Integer) iter.next()) != null) {
                    Double sum = 0.0;

                    //retrieve all parents of target page with childParentMap
                    Integer[] parents = dbManage.childParentMap(pageId);

                    //retrieve pagerank and # of children for each of the above pages
                    for (Integer parent : parents) {
                        sum += score.get(parent)/dbManage.parentChildMap(parent).length;
                        //errorSum += Math.abs(sum - score.get(pageId));
                    }

                    //sum across
                    sum = (1-damp) + damp * sum;
                    //update value
                    score.put(pageId,sum);
                }

                //break clause for low error
                //if (errorSum < maxError*dbManage.pageidCounter) {break;}

            }

            //put in order
           Map<Integer, Float> pageRank = score.entrySet().stream()
                   .sorted(Map.entry.comparingByKey())
                   .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                           (oldValue, newValue) -> oldValue, LinkedHashMap::new));

            return pageRank.keys();
        }
    }
}