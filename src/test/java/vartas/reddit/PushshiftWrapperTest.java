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
package vartas.reddit;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import vartas.offlinejraw.OfflineCommentListingResponse;
import vartas.offlinejraw.OfflineSubmissionListingResponse;
import vartas.reddit.PushshiftWrapper.CompactComment;
import vartas.reddit.PushshiftWrapper.CompactSubmission;
import vartas.xml.collection.XMLMap;
import vartas.xml.collection.XMLMultitable;

/**
 *
 * @author u/Zavarov
 */
public class PushshiftWrapperTest {
    String json = "{\"data\": [{\"id\": \"submission\"}]}";
        
    OfflineSubmissionListingResponse.OfflineSubmission sub = new OfflineSubmissionListingResponse.OfflineSubmission()
            .addNumComments(3)
            .addThumbnailHeight(0)
            .addThumbnailWidth(0)
            .addThumbnail(null)
            .addSelftext("selftext")
            .addLinkFlairText("flair")
            .addOver18(true)
            .addSpoilerTest(true)
            .addLikes()
            .addUrl("sub1.jpg")
            .addTitle("title")
            .addSubredditId("subreddit_id")
            .addSubreddit("subreddit")
            .addPermalink("permalink")
            .addId("id0")
            .addName("submission")
            .addAuthor("author")
            .addDomain("i.redd.it")
            .addDistinguished()
            .addCreatedUtc(1L);

    OfflineCommentListingResponse.OfflineComment com1 = new OfflineCommentListingResponse.OfflineComment()
            .addNumComments(3)
            .addSubredditType("public")
            .addLinkId("link")
            .addParentId("parent")
            .addBody("content")
            .addReplies()
            .addLikes()
            .addUrl("url")
            .addTitle("title")
            .addSubredditId("subreddit_id")
            .addSubreddit("subreddit")
            .addPermalink("permalink")
            .addName("name")
            .addDomain("i.redd.it")
            .addDistinguished()
            .addCreatedUtc(1L)
            .addAuthor("author")
            .addId("id1");

    OfflineCommentListingResponse.OfflineComment com2 = new OfflineCommentListingResponse.OfflineComment()
            .addNumComments(3)
            .addSubredditType("public")
            .addLinkId("link")
            .addParentId("parent")
            .addBody("content")
            .addReplies()
            .addLikes()
            .addUrl("url")
            .addTitle("title")
            .addSubredditId("subreddit_id")
            .addSubreddit("subreddit")
            .addPermalink("permalink")
            .addName("name")
            .addDomain("i.redd.it")
            .addDistinguished()
            .addCreatedUtc(1L)
            .addAuthor("author")
            .addId("id2");

    OfflineCommentListingResponse.OfflineComment com3 = new OfflineCommentListingResponse.OfflineComment()
            .addNumComments(3)
            .addSubredditType("public")
            .addLinkId("link")
            .addParentId("parent")
            .addBody("content")
            .addReplies(com2)
            .addLikes()
            .addUrl("url")
            .addTitle("title")
            .addSubredditId("subreddit_id")
            .addSubreddit("subreddit")
            .addPermalink("permalink")
            .addName("name")
            .addDomain("i.redd.it")
            .addDistinguished()
            .addCreatedUtc(1L)
            .addAuthor("author")
            .addId("id3");

    OfflineRedditBot bot = new OfflineRedditBot();
    
    PushshiftWrapper wrapper;
    Instant instant = Instant.ofEpochMilli(0);
    CompactSubmission submission = new CompactSubmission();
    CompactComment comment = new CompactComment();
    String comments;
    String submissions;
    @Before
    public void setUp(){
        comment.put("author","author");
        comment.put("id","id");
        comment.put("score","1");
        comment.put("submission","submission");
        comment.put("subreddit","subreddit");
        comment.put("title","title");
        
        submission.put("author","author");
        submission.put("id","id");
        submission.put("score","1");
        submission.put("title","title");
        submission.put("link_flair_text","flair");
        submission.put("subreddit","subreddit");
        submission.put("over18", "true");
        submission.put("spoiler","true");
        
        bot.adapter.addResponse(new OfflineCommentListingResponse()
                .addComment(com1)
                .addComment(com3)
                .addSubmission(sub,"subreddit","submission")
                .addAfter(null)
                .addAfterRequest(null)
                .addLimit(1)
                .addSort("new")
                .build());
        
        wrapper = new PushshiftWrapper(bot);
        wrapper.comments.putSingle(instant, "subreddit", comment);
        wrapper.submissions.putSingle(instant, "subreddit", submission);
        
        comments = PushshiftWrapper.COMMENTS_FILE;
        submissions = PushshiftWrapper.SUBMISSIONS_FILE;
        PushshiftWrapper.COMMENTS_FILE = "src/test/resources/comments.xml";
        PushshiftWrapper.SUBMISSIONS_FILE = "src/test/resources/submissions.xml";
    }
    @After
    public void cleanUp(){
        PushshiftWrapper.COMMENTS_FILE = comments;
        PushshiftWrapper.SUBMISSIONS_FILE = submissions;
    }
    
    @Test
    public void parameterTest(){
        Instant before = Instant.ofEpochMilli(0);
        Instant after = Instant.ofEpochMilli(0);
        assertNull(wrapper.after);
        assertNull(wrapper.before);
        assertNull(wrapper.subreddit);
        wrapper.parameter("subreddit", before, after);
        assertEquals(wrapper.subreddit, "subreddit");
        assertEquals(wrapper.before, before);
        assertEquals(wrapper.after, after);
    }
    
    @Test
    public void removeTest(){
        assertFalse(wrapper.submissions.isEmpty());
        assertFalse(wrapper.comments.isEmpty());
        wrapper.remove(instant, "subreddit");
        assertTrue(wrapper.submissions.isEmpty());
        assertTrue(wrapper.comments.isEmpty());
    }
    
    @Test
    public void removeSubredditTest(){
        assertFalse(wrapper.submissions.isEmpty());
        assertFalse(wrapper.comments.isEmpty());
        wrapper.removeSubreddit("subreddit");
        assertTrue(wrapper.submissions.isEmpty());
        assertTrue(wrapper.comments.isEmpty());
    }
    
    @Test
    public void removeDateTest(){
        assertFalse(wrapper.submissions.isEmpty());
        assertFalse(wrapper.comments.isEmpty());
        wrapper.removeDate(instant);
        assertTrue(wrapper.submissions.isEmpty());
        assertTrue(wrapper.comments.isEmpty());
    }
    @Test
    public void getSubmissionsDateTest(){
        Map<String,List<CompactSubmission>> map = Maps.asMap(Sets.newHashSet("subreddit") , (a) -> Arrays.asList(submission));
        assertEquals(map,wrapper.getSubmissions(instant));
    }
    @Test
    public void getSubmissionsSubredditTest(){
        Map<Instant,List<CompactSubmission>> map = Maps.asMap(Sets.newHashSet(instant) , (a) -> Arrays.asList(submission));
        assertEquals(map,wrapper.getSubmissions("subreddit"));
    }
    @Test
    public void getSubmissionsDateSubredditTest(){
        assertEquals(Arrays.asList(submission),wrapper.getSubmissions(instant, "subreddit"));
    }
    @Test
    public void getCommentsDateTest(){
        Map<String,List<CompactComment>> map = Maps.asMap(Sets.newHashSet("subreddit") , (a) -> Arrays.asList(comment));
        assertEquals(map,wrapper.getComments(instant));
    }
    @Test
    public void getCommentsSubredditTest(){
        Map<Instant,List<CompactComment>> map = Maps.asMap(Sets.newHashSet(instant) , (a) -> Arrays.asList(comment));
        assertEquals(map,wrapper.getComments("subreddit"));
    }
    @Test
    public void getCommentsDateSubredditTest(){
        assertEquals(Arrays.asList(comment),wrapper.getComments(instant, "subreddit"));
    }
    @Test
    public void createUrlTest(){
        wrapper.parameter("subreddit", Instant.ofEpochSecond(1),Instant.ofEpochSecond(2));
        String url = String.format("https://api.pushshift.io/reddit/search/submission/?subreddit=subreddit&after=2&before=1&sort=desc&size=500");
        assertEquals(url,wrapper.createUrl());
    }
    @Test
    public void getDatesTest(){
        assertEquals(Arrays.asList(instant),wrapper.getDates());
    }
    @Test
    public void getSubredditsTest(){
        assertEquals(Arrays.asList("subreddit"),wrapper.getSubreddits());
    }
    @Test
    public void storeTest() throws IOException, InterruptedException, ClassNotFoundException{
        PushshiftWrapper.COMMENTS_FILE = "src/test/resources/comments_store.xml";
        PushshiftWrapper.SUBMISSIONS_FILE = "src/test/resources/submissions_store.xml";
        wrapper.store();
        
        XMLMultitable<Instant,String,XMLMap<String,String>> table = wrapper._read(PushshiftWrapper.SUBMISSIONS_FILE);
        assertEquals(table.size(),1);
        assertTrue(table.values().stream().flatMap(e -> e.stream()).collect(Collectors.toList()).contains(submission));
    }
    @Test
    public void storeNewTest() throws IOException, InterruptedException, ClassNotFoundException{
        PushshiftWrapper.COMMENTS_FILE = "src/test/resources/comments_new.xml";
        PushshiftWrapper.SUBMISSIONS_FILE = "src/test/resources/submissions_new.xml";
        wrapper.store();
        
        XMLMultitable<Instant,String,XMLMap<String,String>> table = wrapper._read(PushshiftWrapper.SUBMISSIONS_FILE);
        assertEquals(table.size(),1);
        assertTrue(table.values().stream().flatMap(e -> e.stream()).collect(Collectors.toList()).contains(submission));
        
        File file;
        file = new File(PushshiftWrapper.COMMENTS_FILE);
        file.delete();
        file = new File(PushshiftWrapper.SUBMISSIONS_FILE);
        file.delete();
    }
    @Test
    public void readTest() throws IOException, ClassNotFoundException{
        PushshiftWrapper.COMMENTS_FILE = "src/test/resources/comments.xml";
        PushshiftWrapper.SUBMISSIONS_FILE = "src/test/resources/submissions.xml";
        wrapper.comments.clear();
        wrapper.submissions.clear();
        
        wrapper.read();
        
        assertTrue(wrapper.submissions.values().stream().flatMap(e -> e.stream()).collect(Collectors.toList()).contains(submission));
        assertTrue(wrapper.comments.values().stream().flatMap(e -> e.stream()).collect(Collectors.toList()).contains(comment));
    }
    @Test
    public void readEmptyTest() throws IOException, ClassNotFoundException{
        wrapper.comments.clear();
        wrapper.submissions.clear();
        PushshiftWrapper.COMMENTS_FILE = "src/test/resources/comments_new.xml";
        PushshiftWrapper.SUBMISSIONS_FILE = "src/test/resources/submissions_new.xml";
        
        assertTrue(wrapper.comments.isEmpty());
        assertTrue(wrapper.submissions.isEmpty());
        
        wrapper.read();
        
        assertTrue(wrapper.comments.isEmpty());
        assertTrue(wrapper.submissions.isEmpty());
    }
    @Test
    public void requestJsonContentTest() throws IOException{
        //1st January 2018 00:00:00
        wrapper.parameter("subreddit", Instant.ofEpochSecond(1514764800), Instant.ofEpochSecond(1514764800));
        assertEquals("{    \"data\": []}",wrapper.requestJsonContent());
    }
    @Test
    public void extractIdsTest(){
        assertEquals(wrapper.extractIds(json),Arrays.asList("submission"));
    }
    @Test
    public void requestTest() throws IOException{
        wrapper = new PushshiftWrapper(bot){
            @Override
            public String requestJsonContent(){
                return json;
            }
        };
        
        assertTrue(wrapper.comments.isEmpty());
        assertTrue(wrapper.submissions.isEmpty());
        
        wrapper.parameter("subreddit", Instant.ofEpochSecond(2), Instant.ofEpochSecond(1));
        wrapper.request();
        
        assertEquals(wrapper.submissions.size(),1);
        assertEquals(wrapper.comments.values().stream().flatMap(i -> i.stream()).count(),3);
    
        Set<String> ids = wrapper.submissions.values().stream().flatMap(e -> e.stream()).map(e -> e.getId()).collect(Collectors.toSet());
        assertEquals(ids, Sets.newHashSet("id0"));
        
        ids = wrapper.comments.values().stream().flatMap(e -> e.stream()).map(e -> e.getId()).collect(Collectors.toSet());
        assertEquals(ids, Sets.newHashSet("id1","id2","id3"));
        
        assertEquals(wrapper.submissions.values().iterator().next().get(0).getLinkFlairText(),"flair");
    }
}