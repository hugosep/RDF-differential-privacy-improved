import memoria.hugosepulvedaa.MaxFreqQuery;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MaxFreqQueryTest {

    @Test
    public void initialize() {
        String query = "?var2 <http://www.wikidata.org/prop/direct/P31> ?tmp . \n" +
                "?var2 <http://www.wikidata.org/prop/direct/P585> ?var4 . \n" +
                "?var2 <http://www.wikidata.org/prop/direct/P17> ?var1 . \n";
        MaxFreqQuery maxFreqQuery1 = new MaxFreqQuery(query, "tmp");

        MaxFreqQuery maxFreqQuery2 = new MaxFreqQuery(query, "tmp");
        System.out.println(maxFreqQuery1.equals(maxFreqQuery2));
        //assertEquals(maxFreqQuery1, maxFreqQuery2);
    }
}
