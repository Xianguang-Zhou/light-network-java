/*
 * Copyright (c) 2020, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.zxg.network.core;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
class RingArray {

    private final byte[] content;
    private int first = 0;

    public RingArray(byte[] content) {
        assert content.length > 0;
        this.content = content;
    }

    private int nextIndex(int index) {
        ++index;
        if (content.length == index) {
            index = 0;
        }
        return index;
    }

    public void add(byte element) {
        content[this.first] = element;
        this.first = nextIndex(this.first);
    }

    public boolean isEqual(byte[] other) {
        assert content.length == other.length;
        for (int index = this.first, otherIndex = 0;
             otherIndex < other.length;
             index = nextIndex(index), ++otherIndex) {
            if (content[index] != other[otherIndex]) {
                return false;
            }
        }
        return true;
    }
}
