/*
 * LOP: Low Overhead Profiler
 * Copyright (C) 2014 Florian Kohlmayer, Fabian Prasser
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.deidentifier.arx.example;

import org.deidentifier.arx.profiler.Profiler;

/**
 * Basic example. The associated configuration file can be found in the root directory.
 * 
 * @author Fabian Prasser, Florian Kohlmayer
 */
public class Example extends BaseExample{

    /**
     * Main entry point
     * @param args
     * @throws ClassNotFoundException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws SecurityException
     */
    public static void main(final String[] args) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        final Example c = new Example();
        for (int i=0; i<2000; i++) c.test1();
        Profiler.get().reset();
        for (int i=0; i<2000; i++) c.test2();

    }

    /**
     * Test method
     * @return
     */
    public String test1() {
        System.out.println("test1");
        return "a";
    }

    /**
     * Test method
     * @return
     */
    public String test1(int c) {
        System.out.println("test1");
        return "a";
    }

    /**
     * Test method
     * @return
     */
    public String test1(int c, String h) {
        System.out.println("test1");
        return "a";
    }

    /**
     * Test method
     * @return
     */
    public String test2() {
        System.out.println("test2");
        return "a";
    }
}
