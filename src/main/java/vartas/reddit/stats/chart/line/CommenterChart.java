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
package vartas.reddit.stats.chart.line;

import java.util.Collection;
import vartas.reddit.PushshiftWrapper.CompactComment;

/**
 * This class creates a plot over all unique redditors that made a comment
 * in one of the submissions in the given time frame.
 * @author u/Zavarov
 */
public class CommenterChart extends DevelopmentChart<CompactComment>{
    /**
     * @param data a collection of all comments in the interval.
     * @return the number of unique commenters. 
     */
    @Override
    protected long count(Collection<CompactComment> data) {
        return data.stream().map(CompactComment::getAuthor).distinct().count();
    }
    /**
     * @return the title of the chart. 
     */
    @Override
    protected String getTitle() {
        return "Number of unique commenters";
    }
    /**
     * @return the type of values on the y axis. 
     */
    @Override
    protected String getYLabel() {
        return "#Commenters";
    }
}
