/*
 * Copyright (c) 2020 Zavarov
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

package vartas.reddit.types;

import org.json.JSONObject;
import vartas.reddit.$factory.LinkFactory;
import vartas.reddit.Link;

import java.util.Objects;
import java.util.function.Function;

public class Thing extends ThingTOP {
    public static final Function<Thing,Link> THING2LINK = new Thing2Link();
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String KIND = "kind";
    private static final String DATA = "data";

    @Override
    public String getId() {
        return getSource().getString(ID);
    }

    @Override
    public String getName() {
        return getSource().getString(NAME);
    }

    @Override
    public String getKind() {
        return getSource().getString(KIND);
    }

    @Override
    public JSONObject getData() {
        return getSource().getJSONObject(DATA);
    }

    @Override
    public Thing getRealThis() {
        return this;
    }

    public enum Kind {
        Comment("t1"),
        Account("t2"),
        Link("t3"),
        Message("t4"),
        Subreddit("t5"),
        Award("t6"),
        Listing("Listing");

        private final String name;

        Kind(String name){
            this.name = name;
        }

        public boolean matches(Thing thing){
            return matches(thing.getKind());
        }

        public boolean matches(String kind){
            return Objects.equals(name, kind);
        }
    }

    private static class Thing2Link implements Function<Thing, Link> {
        @Override
        public Link apply(Thing thing) {
            assert Thing.Kind.Link.matches(thing);
            return LinkFactory.create(Link::new, thing.getData());
        }
    }
}