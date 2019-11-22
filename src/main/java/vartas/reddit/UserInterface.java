/*
 * Copyright (c) 2019 Zavarov
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

/**
 * This is the interface for all Reddit users.
 */
public interface UserInterface extends RedditSnowflake{
    /**
     * The URL frame that will link to the subreddit.
     */
    String USER_URL = "https://www.reddit.com/u/%s";

    /**
     * @return the link karma.
     */
    int getCommentKarma();

    /**
     * @return name of the user.
     */
    int getLinkKarma();

    /**
     * @return name of the user.
     */
    String getName();

    /**
     * @return the permalink to the users' page.
     */
    @Override
    default String getPermalink(){
        return String.format(USER_URL, getName());
    }
}
