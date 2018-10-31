/*
 * Copyright (C) 2018 u/Zavarov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package vartas.reddit.stats;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author u/Zavarov
 */
public class AccumulatedSubmissionTest {
    CompactData data;
    AccumulatedSubmission entry;
    @Before
    public void setUp(){
        data = new CompactData();
        entry = new AccumulatedSubmission(data.submissions,2);
    }
    @Test
    public void getTotalTest(){
        assertEquals(entry.getTotal(),5);
    }
    @Test
    public void getRateTest(){
        assertEquals(2.5,entry.getRate(),0.0);
        
    }
    @Test
    public void getUniqueRedditorsTest(){
        assertEquals(entry.getUniqueRedditors(),5);
    }
    @Test
    public void getCombinedScoreTest(){
        assertEquals(entry.getCombinedScore(),15);
    }
}