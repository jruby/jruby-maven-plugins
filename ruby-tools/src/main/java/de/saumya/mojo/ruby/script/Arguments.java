/**
 * 
 */
package de.saumya.mojo.ruby.script;

import java.util.LinkedList;
import java.util.List;

class Arguments {

    final List<String> list = new LinkedList<String>();

    Arguments add(final String name) {
        if (name != null) {
            this.list.add(name);
        }
        return this;
    }

    Arguments add(final String name, final String value) {
        this.list.add(name);
        this.list.add(value);
        return this;
    }

    Arguments parseAndAdd(final String line) {
        if (line != null) {
            for (final String arg : line.trim().split("\\s+")) {
                this.list.add(arg);
            }
        }
        return this;
    }
}