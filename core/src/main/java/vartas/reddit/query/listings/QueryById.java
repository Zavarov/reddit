package vartas.reddit.query.listings;

import com.google.common.base.Joiner;
import org.json.JSONObject;
import vartas.reddit.Client;
import vartas.reddit.Endpoint;
import vartas.reddit.Link;
import vartas.reddit.exceptions.HttpException;
import vartas.reddit.http.APIRequest;
import vartas.reddit.query.Query;
import vartas.reddit.types.Listing;
import vartas.reddit.types.Thing;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class QueryById extends Query<List<Link>, QueryById> {
    public QueryById(Client client, String... names){
        super(client, Endpoint.GET_BY_ID, Joiner.on(",").join(names));
    }

    @Override
    protected QueryById getRealThis() {
        return this;
    }

    @Override
    public List<Link> query() throws InterruptedException, IOException, HttpException {
        String source = new APIRequest.Builder(client)
                .setParams(params)
                .setEndpoint(endpoint)
                .setArgs(args)
                .build()
                .get();
        JSONObject response = new JSONObject(source);

        Listing listing = Thing.from(response).toListing();

        return listing.getChildren().stream().map(Thing::toLink).collect(Collectors.toUnmodifiableList());
    }
}
