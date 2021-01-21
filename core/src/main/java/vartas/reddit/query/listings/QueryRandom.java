package vartas.reddit.query.listings;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import vartas.reddit.Client;
import vartas.reddit.Endpoint;
import vartas.reddit.Link;
import vartas.reddit.exceptions.HttpException;
import vartas.reddit.query.Query;
import vartas.reddit.types.Listing;
import vartas.reddit.types.Thing;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class QueryRandom extends Query<Pair<Link, List<Thing>>,QueryRandom> {
    public QueryRandom(Client client, Endpoint endpoint, Object... args) {
        super(client, endpoint, args);
    }

    @Override
    protected QueryRandom getRealThis() {
        return this;
    }

    @Override
    public Pair<Link, List<Thing>> query() throws IOException, HttpException, InterruptedException {
        JSONArray response = new JSONArray(client.get(params, endpoint, args));

        Link link;
        List<Thing> comments;

        //We receive an array consisting of two listings.
        //The first listing contains a randomly fetched submission
        //The second listing contains comments belonging to the fetched submission
        //Note that it might be the case that the comments are compressed
        assert response.length() == 2;

        //Extract random submissions
        Listing listing = Thing.from(response.getJSONObject(0)).toListing();
        List<Thing> children = listing.getChildren();

        //Reddit should've only returned a single submission
        assert children.size() == 1;

        link = children.get(0).toLink();

        //Extract comments, if present
        listing = Thing.from(response.getJSONObject(1)).toListing();
        comments = Collections.unmodifiableList(listing.getChildren());

        return new ImmutablePair<>(link, comments);
    }
}