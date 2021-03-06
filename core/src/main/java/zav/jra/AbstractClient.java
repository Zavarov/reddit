package zav.jra;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zav.jra._factory.RateLimiterFactory;
import zav.jra._json.JSONToken;
import zav.jra.exceptions._factory.*;
import zav.jra.http.APIAuthentication;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public abstract class AbstractClient extends AbstractClientTOP {
    /**
     * The endpoint for requesting an access token.
     */
    @Nonnull
    protected static final String ACCESS_TOKEN = "https://www.reddit.com/api/v1/access_token";
    /**
     * The endpoint for revoking either an access or a refresh token.
     */
    @Nonnull
    protected static final String REVOKE_TOKEN = "https://www.reddit.com/api/v1/revoke_token";
    /**
     * A random UUID used to identify the hardware.
     */
    @Nonnull
    protected final String uuid = UUID.randomUUID().toString();
    /**
     * A rate limiter to handle sudden bursts of request.
     * Using OAuth2, Reddit only allows us to do up to 60 requests per minute. Violating this rule may get the
     * application banned.
     */
    @Nonnull
    protected final RateLimiter rateLimiter = RateLimiterFactory.create(RateLimiter::new, 0, 60, 60);
    /**
     * The logger for keeping track of the HTTP exchange between Reddit and application.
     */
    @Nonnull
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    /**
     * The actual HTTP client performing the requests.
     */
    @Nonnull
    protected final OkHttpClient http = new OkHttpClient();


    /**
     * Returns a reference to the instance of this class.
     * @return {@code this}.
     */
    @Override
    @SuppressWarnings("all") @Nonnull
    public AbstractClient getRealThis() {
        return this;
    }

    //----------------------------------------------------------------------------------------------------------------//
    //                                                                                                                //
    //    HTTP Requests                                                                                               //
    //                                                                                                                //
    //----------------------------------------------------------------------------------------------------------------//

    /**
     * This function has two purposes. The primary purpose is to execute the provided {@link Request}. However it also
     * checks if the current access token is still valid. In case it expired, a new one will be fetched automatically.
     * @param request The request transmitted to Reddit.
     * @return The HTTP {@link Response} corresponding to the {@link Request}.
     * @throws InterruptedException If the query got interrupted while waiting to be executed.
     * @throws IOException If the request couldn't be completed.
     */
    @Override
    public synchronized Response request(Supplier<Request> request) throws IOException, InterruptedException {
        assert isPresentToken();

        //Make sure that the token is still valid
        if(orElseThrowToken().isExpired())
            refresh();

        return execute(request.get());
    }

    /**
     * This method serves three purposes. The primary purpose is to execute the provided {@link Request}. In addition,
     * it also makes sure that all requests are made within the rate limit and, if necessary, waits until the next
     * {@link Request} can be made.<p>
     * It also checks if the {@link Request} was accepted and, upon error, throws the corresponding exception.
     * @param request The request transmitted to Reddit.
     * @return The HTTP {@link Response} corresponding to the {@link Request}.
     * @throws InterruptedException If the query got interrupted while waiting to be executed.
     * @throws IOException If the request couldn't be completed.
     */
    public synchronized Response execute(Request request) throws InterruptedException, IOException {
        //Wait if we're making too many requests at once
        rateLimiter.acquire();

        log.debug("--> {}", request);
        Response response = http.newCall(request).execute();
        rateLimiter.update(response);
        log.debug("<-- {}", response);
        log.debug("{} used, {} remain, {} seconds until next period", rateLimiter.getUsed(), rateLimiter.getRemaining(), rateLimiter.getReset());


        if(!response.isSuccessful()){
            switch(response.code()){
                //Unauthorized
                case 401:
                    throw UnauthorizedExceptionFactory.create(response.code(), response.message());
                    //Forbidden
                case 403:
                    throw ForbiddenExceptionFactory.create(response.code(), response.message());
                    //Not Found
                case 404:
                    throw NotFoundExceptionFactory.create(response.code(), response.message());
                    //Too Many Requests
                case 429:
                    throw RateLimiterExceptionFactory.create();
                default:
                    throw HttpExceptionFactory.create(response.code(), response.message());
            }
        }

        return response;
    }

    //----------------------------------------------------------------------------------------------------------------//
    //                                                                                                                //
    //    Login                                                                                                       //
    //                                                                                                                //
    //----------------------------------------------------------------------------------------------------------------//

    /**
     * Requests a new access and refresh token.
     * @throws InterruptedException If the query got interrupted while waiting to be executed.
     * @throws IOException If the request couldn't be completed.
     */
    @Override
    public void login() throws InterruptedException, IOException {
        login(Duration.PERMANENT);
    }

    //----------------------------------------------------------------------------------------------------------------//
    //                                                                                                                //
    //    Logout                                                                                                      //
    //                                                                                                                //
    //----------------------------------------------------------------------------------------------------------------//


    /**
     * Invalidates the access token and -if present- the refresh token. It is highly recommended to always invalidate
     * tokens once the they are no longer needed. Not only prevents the token to be misused, in case it gets leaked on
     * accident, but also minimizes the overhead since Reddit can safely delete the tokens from their database.
     * @throws InterruptedException If the query got interrupted while waiting to be executed.
     * @throws IOException If the request couldn't be completed.
     */
    @Override
    public synchronized void logout() throws IOException, InterruptedException {
        assert isPresentToken();

        revokeRefreshToken();
        revokeAccessToken();
        setToken(Optional.empty());
    }

    /**
     * A helper method invalidating the refresh token, if present.
     * @throws InterruptedException If the query got interrupted while waiting to be executed.
     * @throws IOException If the request couldn't be completed.
     */
    private void revokeRefreshToken() throws IOException, InterruptedException {
        assert isPresentToken();

        if(orElseThrowToken().isEmptyRefreshToken())
            return;

        new APIAuthentication.Builder(REVOKE_TOKEN, getCredentials(), this)
                .addParameter("token", orElseThrowToken().orElseThrowRefreshToken())
                .addParameter("token_type_hint", TokenType.REFRESH_TOKEN)
                .build()
                .post();
    }

    /**
     * A helper method invalidating the access token.
     * @throws InterruptedException If the query got interrupted while waiting to be executed.
     * @throws IOException If the request couldn't be completed.
     */
    private void revokeAccessToken() throws IOException, InterruptedException {
        assert isPresentToken();

        new APIAuthentication.Builder(REVOKE_TOKEN, getCredentials(), this)
                .addParameter("token", orElseThrowToken().getAccessToken())
                .addParameter("token_type_hint", TokenType.REFRESH_TOKEN)
                .build()
                .post();
    }

    //----------------------------------------------------------------------------------------------------------------//
    //                                                                                                                //
    //    Refresh                                                                                                     //
    //                                                                                                                //
    //----------------------------------------------------------------------------------------------------------------//

    /**
     * Requests a new access token.
     * @throws InterruptedException If the query got interrupted while waiting to be executed.
     * @throws IOException If the request couldn't be completed.
     */
    @Override
    protected synchronized void refresh() throws IOException, InterruptedException {
        log.info("Refresh access token.");
        assert isPresentToken() && orElseThrowToken().isPresentRefreshToken();

        APIAuthentication request = new APIAuthentication.Builder(ACCESS_TOKEN, getCredentials(), this)
                .addParameter("grant_type", GrantType.REFRESH)
                .addParameter("refresh_token", orElseThrowToken().orElseThrowRefreshToken())
                .build();

        JSONObject response = new JSONObject(request.post());

        Token accessToken = new Token();
        setToken(JSONToken.fromJson(accessToken, response));
        accessToken.setLifetime(response.getInt("expires_in"));
    }

    //----------------------------------------------------------------------------------------------------------------//
    //                                                                                                                //
    //    Utility classes                                                                                             //
    //                                                                                                                //
    //----------------------------------------------------------------------------------------------------------------//

    public enum Duration {
        PERMANENT,
        TEMPORARY;

        @Override
        public String toString(){
            return name().toLowerCase(Locale.ENGLISH);
        }
    }

    public enum GrantType {
        USERLESS("https://oauth.reddit.com/grants/installed_client"),
        PASSWORD("password"),
        CLIENT("client_credentials"),
        REFRESH("refresh_token");

        private final String value;
        GrantType(String value){
            this.value = value;
        }

        @Override
        public String toString(){
            return value;
        }
    }

    /**
     * The token type is used to inform Reddit about the kind of token that is transmitted. It is required when
     * refreshing the access token or invalidating already existing tokens.
     */
    public enum TokenType {
        /**
         * The access token is required to authenticate the application when using the OAuth2 endpoints.
         */
        ACCESS_TOKEN,
        /**
         * The refresh token is required when requesting a new access token, once the previous one expired.
         */
        REFRESH_TOKEN;

        @Override
        public String toString(){
            return name().toLowerCase(Locale.ENGLISH);
        }
    }

    /**
     * A list containing all supported Reddit scopes.
     * See https://www.reddit.com/api/v1/scopes for more.
     */
    @Nonnull
    @SuppressWarnings("unused")
    public enum Scope {
        /**
         * Spend my reddit gold creddits on giving gold to other users.
         */
        CREDDITS("creddits"),
        /**
         * Add/remove users to approved user lists and ban/unban or mute/unmute users from subreddits I moderate.
         */
        MODCONTRIBUTORS("modcontributors"),
        /**
         * Access and manage modmail via mod.reddit.com.
         */
        MODMAIL("modmail"),
        /**
         * Manage the configuration, sidebar, and CSS of subreddits I moderate.
         */
        MODCONFIG("modconfig"),
        /**
         * Manage my subreddit subscriptions. Manage \"friends\" - users whose content I follow.
         */
        SUBSCRIBE("subscribe"),
        /**
         * Edit structured styles for a subreddit I moderate.
         */
        STRUCTUREDSTYLES("structuredstyles"),
        /**
         * Submit and change my votes on comments and submissions.
         */
        VOTE("vote"),
        /**
         * Edit wiki pages on my behalf.
         */
        WIKIEDIT("wikiedit"),
        /**
         * Access the list of subreddits I moderate, contribute to, and subscribe to.
         */
        MYSUBREDDITS("mysubreddits"),
        /**
         * Submit links and comments from my account.
         */
        SUBMIT("submit"),
        /**
         * Access the moderation log in subreddits I moderate.
         */
        MODLOG("modlog"),
        /**
         * Approve, remove, mark nsfw, and distinguish content in subreddits I moderate.
         */
        MODPOST("modpost"),
        /**
         * Manage and assign flair in subreddits I moderate.
         */
        MODFLAIR("modflair"),
        /**
         * Save and unsave comments and submissions.
         */
        SAVE("save"),
        /**
         * Invite or remove other moderators from subreddits I moderate.
         */
        MODOTHERS("modothers"),
        /**
         * Access posts and comments through my account.
         */
        READ("read"),
        /**
         * Access my inbox and send private messages to other users.
         */
        PRIVATEMESSAGES("privatemessages"),
        /**
         * Report content for rules violations. Hide &amp; show individual submissions.
         */
        REPORT("report"),
        /**
         * Access my reddit username and signup date.
         */
        IDENTITY("identity"),
        /**
         * Manage settings and contributors of live threads I contribute to.
         */
        LIVEMANAGE("livemanage"),
        /**
         * Update preferences and related account information. Will not have access to your email or password.
         */
        ACCOUNT("account"),
        /**
         * Access traffic stats in subreddits I moderate.
         */
        MODTRAFFIC("modtraffic"),
        /**
         * Read wiki pages through my account.
         */
        WIKIREAD("wikiread"),
        /**
         * Edit and delete my comments and submissions.
         */
        EDIT("edit"),
        /**
         * Change editors and visibility of wiki pages in subreddits I moderate.
         */
        MODWIKI("modwiki"),
        /**
         * Accept invitations to moderate a subreddit. Remove myself as a moderator or contributor of subreddits I moderate
         * or contribute to.
         */
        MODSELF("modself"),
        /**
         * Access my voting history and comments or submissions I've saved or hidden.
         */
        HISTORY("history"),
        /**
         * Select my subreddit flair. Change link flair on my submissions.
         */
        FLAIR("flair"),

        /**
         * A wildcard indicating that all scopes are used.
         */
        ANY("*");

        /**
         * The scope name.
         */
        @Nonnull
        private final String name;

        /**
         * Assigns each scope a name. The name matches the one used by Reddit.
         * @param name The name of the scope.
         */
        Scope(@Nonnull String name){
            this.name = name;
        }

        @Nonnull
        @Override
        public String toString(){
            return name;
        }
    }
}
